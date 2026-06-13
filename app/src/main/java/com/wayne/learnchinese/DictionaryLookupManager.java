package com.wayne.learnchinese;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DictionaryLookupManager {
    private static final String PREF_NAME = "dictionary_lookup";
    private static final String JUHE_KEY = "juhe_xhzd_key";
    private static final String DEFAULT_JUHE_KEY = BuildConfig.JUHE_XHZD_API_KEY;
    private static final String QUERY_URL = "https://v.juhe.cn/xhzd/query";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;

    private DictionaryLookupManager() {
    }

    public static String getSavedApiKey(Context context) {
        String apiKey = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(JUHE_KEY, "");
        return apiKey == null || apiKey.isEmpty() ? DEFAULT_JUHE_KEY : apiKey;
    }

    public static void saveApiKey(Context context, String apiKey) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(JUHE_KEY, apiKey == null ? "" : apiKey.trim())
                .apply();
    }

    public static DictionaryEntry query(Context context, String character) throws Exception {
        String apiKey = getSavedApiKey(context);
        if (apiKey.isEmpty()) {
            throw new MissingApiKeyException();
        }
        return query(apiKey, character);
    }

    private static DictionaryEntry query(String apiKey, String character) throws Exception {
        String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.name());
        String encodedWord = URLEncoder.encode(character, StandardCharsets.UTF_8.name());
        URL url = new URL(QUERY_URL + "?key=" + encodedKey + "&word=" + encodedWord + "&dtype=json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod("GET");
        try {
            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String body = responseStream == null ? "" : readString(responseStream);
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("查询失败，HTTP " + responseCode + ": " + body);
            }
            return parseResponse(body);
        } finally {
            connection.disconnect();
        }
    }

    private static DictionaryEntry parseResponse(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        int errorCode = root.optInt("error_code", -1);
        String reason = root.optString("reason", "查询失败");
        if (errorCode != 0) {
            throw new DictionaryApiException(errorCode, reason);
        }
        JSONObject result = root.optJSONObject("result");
        if (result == null) {
            throw new IOException("字典接口返回内容为空");
        }
        return new DictionaryEntry(
                result.optString("zi"),
                result.optString("py"),
                result.optString("pinyin"),
                result.optString("wubi"),
                result.optString("bushou"),
                result.optString("bihua"),
                readStringArray(result.optJSONArray("jijie")),
                readStringArray(result.optJSONArray("xiangjie")));
    }

    private static List<String> readStringArray(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "");
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static String readString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }

    public static class DictionaryEntry {
        final String zi;
        final String py;
        final String pinyin;
        final String wubi;
        final String bushou;
        final String bihua;
        final List<String> simpleExplanations;
        final List<String> detailedExplanations;

        DictionaryEntry(
                String zi,
                String py,
                String pinyin,
                String wubi,
                String bushou,
                String bihua,
                List<String> simpleExplanations,
                List<String> detailedExplanations) {
            this.zi = zi;
            this.py = py;
            this.pinyin = pinyin;
            this.wubi = wubi;
            this.bushou = bushou;
            this.bihua = bihua;
            this.simpleExplanations = simpleExplanations;
            this.detailedExplanations = detailedExplanations;
        }
    }

    public static class MissingApiKeyException extends Exception {
    }

    public static class DictionaryApiException extends Exception {
        final int errorCode;

        DictionaryApiException(int errorCode, String reason) {
            super(reason);
            this.errorCode = errorCode;
        }
    }
}
