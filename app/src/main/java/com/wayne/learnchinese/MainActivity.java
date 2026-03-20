package com.wayne.learnchinese;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private CharacterLibrary library;
    private TextView characterDisplay;
    private TextView indexDisplay;
    private TextView libraryStatsText;
    private TextInputEditText searchInput;
    private FloatingActionButton toggleMarkButton;
    private Random random = new Random();
    private ActivityResultLauncher<Intent> libraryLauncher;

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
                    libraryLauncher.launch(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting LibraryViewActivity", e);
                    Toast.makeText(this, "无法打开字库浏览", Toast.LENGTH_SHORT).show();
                }
            });

            // 设置随机按钮点击事件
            randomButton.setOnClickListener(v -> showRandomCharacter());

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