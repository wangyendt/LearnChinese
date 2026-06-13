package com.wayne.learnchinese;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private CharacterLibrary library;
    private TextView characterDisplay;
    private TextView indexDisplay;
    private TextView libraryStatsText;
    private TextInputEditText searchInput;
    private FloatingActionButton toggleMarkButton;
    private MaterialButton lookupDictionaryButton;
    private MaterialButton uploadConfigButton;
    private MaterialButton downloadConfigButton;
    private Random random = new Random();
    private ActivityResultLauncher<Intent> libraryLauncher;
    private final ExecutorService configSyncExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);
            
            // 初始化视图
            characterDisplay = findViewById(R.id.characterDisplay);
            indexDisplay = findViewById(R.id.indexDisplay);
            searchInput = findViewById(R.id.searchInput);
            libraryStatsText = findViewById(R.id.libraryStatsText);
            FloatingActionButton addButton = findViewById(R.id.addCharacterButton);
            MaterialButton viewLibraryButton = findViewById(R.id.viewLibraryButton);
            MaterialButton randomButton = findViewById(R.id.randomButton);
            lookupDictionaryButton = findViewById(R.id.lookupDictionaryButton);
            uploadConfigButton = findViewById(R.id.uploadConfigButton);
            downloadConfigButton = findViewById(R.id.downloadConfigButton);
            toggleMarkButton = findViewById(R.id.toggleMarkButton);

            if (viewLibraryButton == null) {
                Log.e(TAG, "viewLibraryButton not found");
                return;
            }

            // 初始化字库
            library = new CharacterLibrary(this);
            updateLibraryStats();

            // 初始化结果接收器
            libraryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String character = result.getData().getStringExtra(LibraryViewActivity.EXTRA_SELECTED_CHARACTER);
                        if (character != null) {
                            searchInput.setText(character);
                        }
                    }
                }
            );

            // 设置搜索输入监听
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        String character = s.toString();
                        updateCharacterDisplay(character);
                    } else {
                        clearDisplay();
                    }
                }
            });

            // 设置添加按钮点击事件
            addButton.setOnClickListener(v -> {
                try {
                    showAddCharacterDialog();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing add dialog", e);
                    Toast.makeText(this, "添加功能出现错误", Toast.LENGTH_SHORT).show();
                }
            });

            // 设置查看字库按钮点击事件
            viewLibraryButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, LibraryViewActivity.class);
                    String character = characterDisplay.getText().toString();
                    if (!character.isEmpty() && library.containsCharacter(character)) {
                        intent.putExtra(LibraryViewActivity.EXTRA_INITIAL_CHARACTER, character);
                    }
                    libraryLauncher.launch(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting LibraryViewActivity", e);
                    Toast.makeText(this, "无法打开字库浏览", Toast.LENGTH_SHORT).show();
                }
            });

            // 设置随机按钮点击事件
            randomButton.setOnClickListener(v -> showRandomCharacter());

            lookupDictionaryButton.setOnClickListener(v -> lookupCurrentCharacter());
            uploadConfigButton.setOnClickListener(v -> showUploadConfigDialog());
            downloadConfigButton.setOnClickListener(v -> showDownloadConfigDialog());

            // 设置标记按钮点击事件
            toggleMarkButton.setOnClickListener(v -> {
                String character = characterDisplay.getText().toString();
                if (character.isEmpty()) {
                    Toast.makeText(this, "请先输入或选择一个汉字", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (library.toggleMark(character)) {
                    updateCharacterDisplay(character);
                    Toast.makeText(this, library.isMarked(character) ? "已标记" : "已取消标记", Toast.LENGTH_SHORT).show();
                }
            });

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "应用启动出现错误", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新创建字库实例以获取最新数据
        library = new CharacterLibrary(this);
        updateLibraryStats();
        // 如果搜索框有内容，更新显示
        if (searchInput.length() > 0) {
            updateCharacterDisplay(searchInput.getText().toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        configSyncExecutor.shutdownNow();
    }

    private void updateCharacterDisplay(String character) {
        if (library.containsCharacter(character)) {
            characterDisplay.setText(character);
            if (library.isMarked(character)) {
                characterDisplay.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                characterDisplay.setTextColor(getResources().getColor(android.R.color.black));
            }
            int index = library.findCharacterIndex(character);
            indexDisplay.setText("编号：" + index);
            updateMarkButton(character);
        } else {
            characterDisplay.setText(character);
            characterDisplay.setTextColor(getResources().getColor(android.R.color.black));
            indexDisplay.setText("此字不在字库中");
            toggleMarkButton.setImageResource(android.R.drawable.btn_star);
        }
    }

    private void updateMarkButton(String character) {
        if (library.isMarked(character)) {
            toggleMarkButton.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            toggleMarkButton.setImageResource(android.R.drawable.btn_star);
        }
    }

    private void clearDisplay() {
        characterDisplay.setText("");
        characterDisplay.setTextColor(getResources().getColor(android.R.color.black));
        indexDisplay.setText("");
        toggleMarkButton.setImageResource(android.R.drawable.btn_star);
    }

    private void updateLibraryStats() {
        int total = library.getCharacterCount();
        int marked = library.getMarkedCharacters().size();
        libraryStatsText.setText(String.format("字库中共有 %d 个汉字，已标记 %d 个", total, marked));
    }

    private void showUploadConfigDialog() {
        setSyncButtonsEnabled(false);
        Toast.makeText(this, "正在读取配置日期", Toast.LENGTH_SHORT).show();
        configSyncExecutor.execute(() -> {
            try {
                String localTime = ConfigSyncManager.getLocalConfigModifiedTime(getApplicationContext());
                ConfigSyncManager.RemoteConfigInfo remoteInfo =
                        ConfigSyncManager.fetchRemoteConfigInfoForUpload();
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("上传配置？")
                            .setMessage("本地配置：" + localTime
                                    + "\n云端配置：" + remoteInfo.modifiedTimeText
                                    + "\n\n上传会覆盖云端配置文件。")
                            .setPositiveButton("上传覆盖", (dialog, which) -> uploadConfigToCloud())
                            .setNegativeButton("取消", null)
                            .show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading config info before upload", e);
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    showErrorDialog("读取配置日期失败", e);
                });
            }
        });
    }

    private void uploadConfigToCloud() {
        setSyncButtonsEnabled(false);
        Toast.makeText(this, "正在上传配置", Toast.LENGTH_SHORT).show();
        configSyncExecutor.execute(() -> {
            try {
                ConfigSyncManager.UploadResult result =
                        ConfigSyncManager.uploadConfig(getApplicationContext());
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    Toast.makeText(
                            this,
                            "上传成功：" + result.byteCount + " 字节",
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error uploading config", e);
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    showErrorDialog("上传配置失败", e);
                });
            }
        });
    }

    private void showDownloadConfigDialog() {
        setSyncButtonsEnabled(false);
        Toast.makeText(this, "正在读取云端配置日期", Toast.LENGTH_SHORT).show();
        configSyncExecutor.execute(() -> {
            try {
                String localTime = ConfigSyncManager.getLocalConfigModifiedTime(getApplicationContext());
                ConfigSyncManager.RemoteConfigInfo remoteInfo =
                        ConfigSyncManager.fetchRemoteConfigInfo();
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("覆盖本地配置？")
                            .setMessage("本地配置：" + localTime
                                    + "\n云端配置：" + remoteInfo.modifiedTimeText
                                    + "\n\n下载会覆盖本机已学习和已标记数据。")
                            .setPositiveButton("覆盖本地", (dialog, which) -> downloadConfigFromCloud())
                            .setNegativeButton("取消", null)
                            .show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading remote config info", e);
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    showErrorDialog("读取云端配置失败", e);
                });
            }
        });
    }

    private void downloadConfigFromCloud() {
        setSyncButtonsEnabled(false);
        Toast.makeText(this, "正在下载配置", Toast.LENGTH_SHORT).show();
        configSyncExecutor.execute(() -> {
            try {
                ConfigSyncManager.DownloadResult result =
                        ConfigSyncManager.downloadAndApplyConfig(getApplicationContext());
                mainHandler.post(() -> {
                    library = new CharacterLibrary(this);
                    updateLibraryStats();
                    if (searchInput.length() > 0) {
                        updateCharacterDisplay(searchInput.getText().toString());
                    }
                    setSyncButtonsEnabled(true);
                    Toast.makeText(
                            this,
                            String.format("下载成功：字库 %d 个，已标记 %d 个",
                                    library.getCharacterCount(),
                                    result.markedCharacterCount),
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error downloading config", e);
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    showErrorDialog("下载配置失败", e);
                });
            }
        });
    }

    private void setSyncButtonsEnabled(boolean enabled) {
        lookupDictionaryButton.setEnabled(enabled);
        uploadConfigButton.setEnabled(enabled);
        downloadConfigButton.setEnabled(enabled);
    }

    private void lookupCurrentCharacter() {
        String character = characterDisplay.getText().toString();
        if (character.length() != 1) {
            Toast.makeText(this, "请先输入或选择一个汉字", Toast.LENGTH_SHORT).show();
            return;
        }
        if (DictionaryLookupManager.getSavedApiKey(this).isEmpty()) {
            showDictionaryApiKeyDialog(character);
            return;
        }
        queryDictionary(character);
    }

    private void showDictionaryApiKeyDialog(String character) {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("请输入聚合数据 API Key");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        String savedKey = DictionaryLookupManager.getSavedApiKey(this);
        if (!savedKey.isEmpty()) {
            input.setText(savedKey);
            input.setSelection(savedKey.length());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("设置字典 API Key")
                .setMessage("新华字典接口需要聚合数据 key。Key 会保存在本机应用私有配置中。")
                .setView(createPaddedContainer(input))
                .setPositiveButton("保存并查询", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String apiKey = input.getText() == null ? "" : input.getText().toString().trim();
                    if (apiKey.isEmpty()) {
                        input.setError("请输入 API Key");
                        return;
                    }
                    DictionaryLookupManager.saveApiKey(this, apiKey);
                    dialog.dismiss();
                    queryDictionary(character);
                }));
        dialog.show();
    }

    private void queryDictionary(String character) {
        setSyncButtonsEnabled(false);
        Toast.makeText(this, "正在查询字典", Toast.LENGTH_SHORT).show();
        configSyncExecutor.execute(() -> {
            try {
                DictionaryLookupManager.DictionaryEntry entry =
                        DictionaryLookupManager.query(getApplicationContext(), character);
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    showDictionaryResultDialog(entry);
                });
            } catch (DictionaryLookupManager.MissingApiKeyException e) {
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    showDictionaryApiKeyDialog(character);
                });
            } catch (DictionaryLookupManager.DictionaryApiException e) {
                Log.e(TAG, "Dictionary API error", e);
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    showDictionaryApiErrorDialog(character, e);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error querying dictionary", e);
                mainHandler.post(() -> {
                    setSyncButtonsEnabled(true);
                    showErrorDialog("查询字典失败", e);
                });
            }
        });
    }

    private void showDictionaryApiErrorDialog(
            String character,
            DictionaryLookupManager.DictionaryApiException exception) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("查询字典失败")
                .setMessage("错误码：" + exception.errorCode + "\n" + exception.getMessage())
                .setPositiveButton("修改Key", (dialog, which) -> showDictionaryApiKeyDialog(character))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showDictionaryResultDialog(DictionaryLookupManager.DictionaryEntry entry) {
        TextView content = new TextView(this);
        content.setText(buildDictionaryContent(entry));
        content.setTextSize(17);
        content.setLineSpacing(0, 1.18f);
        content.setTextColor(getResources().getColor(android.R.color.black));

        ScrollView scrollView = new ScrollView(this);
        int padding = (int) (18 * getResources().getDisplayMetrics().density);
        scrollView.setPadding(padding, 0, padding, 0);
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(this)
                .setTitle("字典：" + safe(entry.zi))
                .setView(scrollView)
                .setPositiveButton("关闭", null)
                .show();
    }

    private String buildDictionaryContent(DictionaryLookupManager.DictionaryEntry entry) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "拼音", entry.py);
        appendLine(builder, "读音", entry.pinyin);
        appendLine(builder, "五笔", entry.wubi);
        appendLine(builder, "部首", entry.bushou);
        appendLine(builder, "笔画", entry.bihua);
        appendSection(builder, "简解", entry.simpleExplanations);
        appendSection(builder, "详解", entry.detailedExplanations);
        return builder.toString().trim();
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        builder.append(label).append("：").append(value).append("\n");
    }

    private void appendSection(StringBuilder builder, String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        builder.append("\n").append(title).append("\n");
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                builder.append(value).append("\n");
            }
        }
    }

    private View createPaddedContainer(View child) {
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, 0, padding, 0);
        container.addView(child, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return container;
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "未知" : value;
    }

    private void showErrorDialog(String title, Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isEmpty()) {
            message = "请检查网络或云端链接权限";
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    private void showAddCharacterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_character, null);
        TextInputEditText input = dialogView.findViewById(R.id.addCharacterInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("添加新汉字")
                .setView(dialogView)
                .setPositiveButton("添加", (dialog, which) -> {
                    String character = input.getText().toString();
                    if (character.length() == 1) {
                        if (library.addCharacter(character)) {
                            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
                            searchInput.setText(character);
                            updateLibraryStats();
                        } else {
                            Toast.makeText(this, "该字已存在", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "请输入一个汉字", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRandomCharacter() {
        List<String> characters = library.getAllCharacters();
        if (characters.isEmpty()) {
            Toast.makeText(this, "字库为空", Toast.LENGTH_SHORT).show();
            return;
        }
        String randomChar = characters.get(random.nextInt(characters.size()));
        searchInput.setText(randomChar);
    }
}
