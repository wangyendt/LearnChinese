package com.wayne.learnchinese;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

public class CharacterLibrary {
    private static final String TAG = "CharacterLibrary";
    private static final String PREF_NAME = "character_library";
    private static final String EXTRA_CHARS_KEY = "extra_characters";
    private static final String MARKED_CHARS_KEY = "marked_characters";
    private List<String> characters;
    private Set<String> markedCharacters;
    private SharedPreferences preferences;
    private Context context;

    public CharacterLibrary(Context context) {
        this.context = context;
        characters = new ArrayList<>();
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        markedCharacters = new HashSet<>(preferences.getStringSet(MARKED_CHARS_KEY, new HashSet<>()));
        loadCharacters(context);
    }

    private void loadCharacters(Context context) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getResources().getAssets().open("chinese.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                for (char c : line.toCharArray()) {
                    characters.add(String.valueOf(c));
                }
            }
            reader.close();

            // 加载用户添加的额外汉字
            String extraChars = preferences.getString(EXTRA_CHARS_KEY, "");
            for (char c : extraChars.toCharArray()) {
                if (!characters.contains(String.valueOf(c))) {
                    characters.add(String.valueOf(c));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportLibrary() {
        try {
            // 创建导出目录
            File exportDir = new File(context.getExternalFilesDir(null), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // 创建导出文件
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File exportFile = new File(exportDir, "chinese_" + timestamp + ".txt");

            // 写入字库内容
            FileWriter writer = new FileWriter(exportFile);
            for (String character : characters) {
                writer.write(character);
            }
            writer.close();

            // 创建分享意图
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            
            // 获取文件URI
            Uri fileUri = FileProvider.getUriForFile(context, 
                context.getPackageName() + ".fileprovider", 
                exportFile);
            
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 启动分享
            context.startActivity(Intent.createChooser(shareIntent, "分享字库文件"));

        } catch (Exception e) {
            Log.e(TAG, "Error exporting library", e);
            Toast.makeText(context, "导出字库失败", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean toggleMark(String character) {
        if (character == null || character.isEmpty()) {
            return false;
        }

        if (markedCharacters.contains(character)) {
            markedCharacters.remove(character);
        } else {
            markedCharacters.add(character);
        }
        
        // 保存标记状态
        preferences.edit().putStringSet(MARKED_CHARS_KEY, markedCharacters).apply();
        return true;
    }

    public boolean isMarked(String character) {
        return markedCharacters.contains(character);
    }

    public Set<String> getMarkedCharacters() {
        return new HashSet<>(markedCharacters);
    }

    public int findCharacterIndex(String character) {
        return characters.indexOf(character) + 1;
    }

    public boolean addCharacter(String character) {
        if (character == null || character.length() != 1) {
            return false;
        }
        
        if (!characters.contains(character)) {
            characters.add(character);
            String extraChars = preferences.getString(EXTRA_CHARS_KEY, "");
            extraChars += character;
            preferences.edit().putString(EXTRA_CHARS_KEY, extraChars).apply();
            return true;
        }
        return false;
    }

    public boolean containsCharacter(String character) {
        return characters.contains(character);
    }

    public List<String> getAllCharacters() {
        return new ArrayList<>(characters);
    }

    public int getCharacterCount() {
        return characters.size();
    }

    public boolean deleteCharacter(String character) {
        if (character == null || character.isEmpty()) {
            return false;
        }

        // 从内存中的列表移除
        if (characters.remove(character)) {
            // 重新构建额外字符串
            StringBuilder extraChars = new StringBuilder();
            for (String c : characters) {
                // 如果这个字符不在原始字库中，就添加到额外字符中
                if (!isInOriginalLibrary(c)) {
                    extraChars.append(c);
                }
            }
            // 保存更新后的额外字符
            preferences.edit().putString(EXTRA_CHARS_KEY, extraChars.toString()).apply();
            
            // 如果字符被标记，也要移除标记
            if (markedCharacters.contains(character)) {
                markedCharacters.remove(character);
                preferences.edit().putStringSet(MARKED_CHARS_KEY, markedCharacters).apply();
            }
            return true;
        }
        return false;
    }

    private boolean isInOriginalLibrary(String character) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getResources().getAssets().open("chinese.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(character)) {
                    reader.close();
                    return true;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
} 