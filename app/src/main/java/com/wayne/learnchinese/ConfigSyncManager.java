package com.wayne.learnchinese;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ConfigSyncManager {
    public static final String CLOUD_CONFIG_URL =
            "https://wangye-main-bucket.oss-cn-shenzhen.aliyuncs.com/LearnChinese/character_library.xml";

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String UNKNOWN_TIME = "未知";

    private ConfigSyncManager() {
    }

    public static RemoteConfigInfo fetchRemoteConfigInfo() throws IOException {
        return fetchRemoteConfigInfo(false);
    }

    public static RemoteConfigInfo fetchRemoteConfigInfoForUpload() throws IOException {
        return fetchRemoteConfigInfo(true);
    }

    private static RemoteConfigInfo fetchRemoteConfigInfo(boolean allowMissing) throws IOException {
        HttpURLConnection connection = openConnection("HEAD");
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                if (allowMissing) {
                    return new RemoteConfigInfo(responseCode, "云端暂无配置文件");
                }
                throw new IOException("云端暂无配置文件");
            }
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("读取云端配置日期失败，HTTP " + responseCode);
            }
            return new RemoteConfigInfo(responseCode, formatRemoteModifiedTime(connection));
        } finally {
            connection.disconnect();
        }
    }

    public static UploadResult uploadConfig(Context context) throws Exception {
        byte[] payload = readLocalConfigPayload(context);
        HttpURLConnection connection = openConnection("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/xml; charset=utf-8");
        connection.setFixedLengthStreamingMode(payload.length);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload);
        }

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("上传配置失败，HTTP " + responseCode + ": " + readError(connection));
            }
            return new UploadResult(responseCode, payload.length, getLocalConfigModifiedTime(context));
        } finally {
            connection.disconnect();
        }
    }

    public static DownloadResult downloadAndApplyConfig(Context context) throws Exception {
        HttpURLConnection connection = openConnection("GET");
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("下载配置失败，HTTP " + responseCode + ": " + readError(connection));
            }

            byte[] payload = readAll(connection.getInputStream());
            ConfigData configData = parseConfig(payload);
            applyConfig(context, configData);
            return new DownloadResult(
                    responseCode,
                    formatRemoteModifiedTime(connection),
                    configData.extraCharacters.length(),
                    configData.markedCharacters.size());
        } finally {
            connection.disconnect();
        }
    }

    public static String getLocalConfigModifiedTime(Context context) {
        File configFile = getLocalConfigFile(context);
        if (!configFile.exists()) {
            return "本地暂无配置文件";
        }
        return formatTime(configFile.lastModified());
    }

    private static HttpURLConnection openConnection(String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(CLOUD_CONFIG_URL).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod(method);
        return connection;
    }

    private static File getLocalConfigFile(Context context) {
        return new File(context.getApplicationInfo().dataDir
                + "/shared_prefs/" + CharacterLibrary.PREF_NAME + ".xml");
    }

    private static byte[] readLocalConfigPayload(Context context) throws Exception {
        return buildConfigPayload(context);
    }

    private static byte[] buildConfigPayload(Context context) throws Exception {
        SharedPreferences preferences = context.getSharedPreferences(
                CharacterLibrary.PREF_NAME, Context.MODE_PRIVATE);
        String extraCharacters = preferences.getString(CharacterLibrary.EXTRA_CHARS_KEY, "");
        Set<String> markedCharacters = preferences.getStringSet(
                CharacterLibrary.MARKED_CHARS_KEY, new HashSet<>());

        StringWriter writer = new StringWriter();
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(writer);
        serializer.startDocument("utf-8", true);
        serializer.startTag(null, "map");
        serializer.startTag(null, "string");
        serializer.attribute(null, "name", CharacterLibrary.EXTRA_CHARS_KEY);
        serializer.text(extraCharacters == null ? "" : extraCharacters);
        serializer.endTag(null, "string");
        serializer.startTag(null, "set");
        serializer.attribute(null, "name", CharacterLibrary.MARKED_CHARS_KEY);
        if (markedCharacters != null) {
            for (String character : markedCharacters) {
                serializer.startTag(null, "string");
                serializer.text(character == null ? "" : character);
                serializer.endTag(null, "string");
            }
        }
        serializer.endTag(null, "set");
        serializer.endTag(null, "map");
        serializer.endDocument();
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static ConfigData parseConfig(byte[] payload) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new ByteArrayInputStream(payload), StandardCharsets.UTF_8.name());

        boolean sawMap = false;
        boolean inMarkedSet = false;
        String extraCharacters = "";
        Set<String> markedCharacters = new HashSet<>();

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                String name = parser.getAttributeValue(null, "name");
                if ("map".equals(tagName)) {
                    sawMap = true;
                } else if ("string".equals(tagName)
                        && CharacterLibrary.EXTRA_CHARS_KEY.equals(name)) {
                    extraCharacters = parser.nextText();
                } else if ("set".equals(tagName)
                        && CharacterLibrary.MARKED_CHARS_KEY.equals(name)) {
                    inMarkedSet = true;
                } else if (inMarkedSet && "string".equals(tagName)) {
                    markedCharacters.add(parser.nextText());
                }
            } else if (eventType == XmlPullParser.END_TAG
                    && inMarkedSet
                    && "set".equals(parser.getName())) {
                inMarkedSet = false;
            }
            eventType = parser.next();
        }

        if (!sawMap) {
            throw new IOException("配置文件格式不正确");
        }
        return new ConfigData(extraCharacters == null ? "" : extraCharacters, markedCharacters);
    }

    private static void applyConfig(Context context, ConfigData configData) throws IOException {
        SharedPreferences preferences = context.getSharedPreferences(
                CharacterLibrary.PREF_NAME, Context.MODE_PRIVATE);
        boolean committed = preferences.edit()
                .clear()
                .putString(CharacterLibrary.EXTRA_CHARS_KEY, configData.extraCharacters)
                .putStringSet(CharacterLibrary.MARKED_CHARS_KEY, configData.markedCharacters)
                .commit();
        if (!committed) {
            throw new IOException("写入本地配置失败");
        }
    }

    private static String formatRemoteModifiedTime(HttpURLConnection connection) {
        long modifiedTime = connection.getHeaderFieldDate("Last-Modified", -1);
        if (modifiedTime <= 0) {
            modifiedTime = connection.getLastModified();
        }
        if (modifiedTime <= 0) {
            return UNKNOWN_TIME;
        }
        return formatTime(modifiedTime);
    }

    private static String formatTime(long timestamp) {
        DateFormat format = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        return format.format(new Date(timestamp));
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static String readError(HttpURLConnection connection) throws IOException {
        InputStream errorStream = connection.getErrorStream();
        if (errorStream == null) {
            return "";
        }
        return new String(readAll(errorStream), StandardCharsets.UTF_8);
    }

    private static class ConfigData {
        final String extraCharacters;
        final Set<String> markedCharacters;

        ConfigData(String extraCharacters, Set<String> markedCharacters) {
            this.extraCharacters = extraCharacters;
            this.markedCharacters = markedCharacters;
        }
    }

    public static class RemoteConfigInfo {
        final int responseCode;
        final String modifiedTimeText;

        RemoteConfigInfo(int responseCode, String modifiedTimeText) {
            this.responseCode = responseCode;
            this.modifiedTimeText = modifiedTimeText;
        }
    }

    public static class UploadResult {
        final int responseCode;
        final int byteCount;
        final String localModifiedTimeText;

        UploadResult(int responseCode, int byteCount, String localModifiedTimeText) {
            this.responseCode = responseCode;
            this.byteCount = byteCount;
            this.localModifiedTimeText = localModifiedTimeText;
        }
    }

    public static class DownloadResult {
        final int responseCode;
        final String cloudModifiedTimeText;
        final int extraCharacterCount;
        final int markedCharacterCount;

        DownloadResult(
                int responseCode,
                String cloudModifiedTimeText,
                int extraCharacterCount,
                int markedCharacterCount) {
            this.responseCode = responseCode;
            this.cloudModifiedTimeText = cloudModifiedTimeText;
            this.extraCharacterCount = extraCharacterCount;
            this.markedCharacterCount = markedCharacterCount;
        }
    }
}
