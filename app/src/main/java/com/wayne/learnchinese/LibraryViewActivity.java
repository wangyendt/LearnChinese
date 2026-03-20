package com.wayne.learnchinese;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import java.util.HashSet;
import java.util.Set;

public class LibraryViewActivity extends AppCompatActivity {
    private CharacterLibrary library;
    private CharacterAdapter adapter;
    private TextView statsText;
    private GridView gridView;
    private View batchActionContainer;
    private boolean isBatchMode = false;
    private Set<String> selectedCharacters = new HashSet<>();
    private boolean isDragging = false;
    private boolean isDragSelecting = false;
    public static final String EXTRA_SELECTED_CHARACTER = "selected_character";

    public boolean isCharacterSelected(String character) {
        return selectedCharacters.contains(character);
    }

    public boolean isBatchMode() {
        return isBatchMode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_view);

        library = new CharacterLibrary(this);
        statsText = findViewById(R.id.statsText);
        gridView = findViewById(R.id.characterGrid);
        batchActionContainer = findViewById(R.id.batchActionContainer);
        MaterialButton toggleBatchModeButton = findViewById(R.id.toggleBatchModeButton);
        MaterialButton markSelectedButton = findViewById(R.id.markSelectedButton);
        MaterialButton unmarkSelectedButton = findViewById(R.id.unmarkSelectedButton);

        adapter = new CharacterAdapter(this, library.getAllCharacters());
        gridView.setAdapter(adapter);

        updateStats();

        // 设置跳转到底部按钮
        FloatingActionButton scrollToBottomButton = findViewById(R.id.scrollToBottomButton);
        scrollToBottomButton.setOnClickListener(v -> {
            int lastPosition = adapter.getCount() - 1;
            if (lastPosition >= 0) {
                gridView.setSelection(lastPosition);
            }
        });

        // 设置批量标记模式切换
        toggleBatchModeButton.setOnClickListener(v -> {
            isBatchMode = !isBatchMode;
            toggleBatchModeButton.setText(isBatchMode ? "退出批量" : "批量标记");
            batchActionContainer.setVisibility(isBatchMode ? View.VISIBLE : View.GONE);
            selectedCharacters.clear();
            adapter.notifyDataSetChanged();
        });

        // 标记选中的字
        markSelectedButton.setOnClickListener(v -> {
            if (selectedCharacters.isEmpty()) {
                Toast.makeText(this, "请先选择要标记的汉字", Toast.LENGTH_SHORT).show();
                return;
            }
            for (String character : selectedCharacters) {
                if (!library.isMarked(character)) {
                    library.toggleMark(character);
                }
            }
            Toast.makeText(this, "已标记 " + selectedCharacters.size() + " 个汉字", Toast.LENGTH_SHORT).show();
            selectedCharacters.clear();
            adapter.notifyDataSetChanged();
            updateStats();
        });

        // 取消标记选中的字
        unmarkSelectedButton.setOnClickListener(v -> {
            if (selectedCharacters.isEmpty()) {
                Toast.makeText(this, "请先选择要取消标记的汉字", Toast.LENGTH_SHORT).show();
                return;
            }
            for (String character : selectedCharacters) {
                if (library.isMarked(character)) {
                    library.toggleMark(character);
                }
            }
            Toast.makeText(this, "已取消标记 " + selectedCharacters.size() + " 个汉字", Toast.LENGTH_SHORT).show();
            selectedCharacters.clear();
            adapter.notifyDataSetChanged();
            updateStats();
        });

        // 设置触摸事件处理
        gridView.setOnTouchListener((v, event) -> {
            if (!isBatchMode) return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isDragging = true;
                    handleGridTouch(event);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        handleGridTouch(event);
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    isDragSelecting = false;
                    return true;
            }
            return false;
        });

        // 网格点击事件
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            String character = adapter.getItem(position);
            if (isBatchMode) {
                // 批量模式下，点击切换选中状态
                if (selectedCharacters.contains(character)) {
                    selectedCharacters.remove(character);
                } else {
                    selectedCharacters.add(character);
                }
                adapter.notifyDataSetChanged();
            } else {
                // 非批量模式下，返回主界面
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SELECTED_CHARACTER, character);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        // 添加长按删除功能
        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (isBatchMode) {
                return false; // 批量模式下禁用长按删除
            }
            String character = adapter.getItem(position);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("删除汉字")
                    .setMessage("确定要删除「" + character + "」吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        if (library.deleteCharacter(character)) {
                            adapter.updateData(library.getAllCharacters());
                            updateStats();
                            Toast.makeText(this, "已删除：" + character, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }

    private void handleGridTouch(MotionEvent event) {
        int position = gridView.pointToPosition((int) event.getX(), (int) event.getY());
        if (position != GridView.INVALID_POSITION) {
            String character = adapter.getItem(position);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isDragSelecting = !selectedCharacters.contains(character);
            }
            if (isDragSelecting) {
                selectedCharacters.add(character);
            } else {
                selectedCharacters.remove(character);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void updateStats() {
        int total = library.getCharacterCount();
        int marked = library.getMarkedCharacters().size();
        statsText.setText(String.format("字库中共有 %d 个汉字，已标记 %d 个", total, marked));
    }
}

class CharacterAdapter extends android.widget.ArrayAdapter<String> {
    private CharacterLibrary library;
    private LibraryViewActivity activity;

    public CharacterAdapter(LibraryViewActivity context, java.util.List<String> characters) {
        super(context, R.layout.grid_item_character, characters);
        this.activity = context;
        library = new CharacterLibrary(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            textView = new TextView(getContext());
            textView.setTextSize(24);
            textView.setPadding(8, 8, 8, 8);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } else {
            textView = (TextView) convertView;
        }
        String character = getItem(position);
        textView.setText(character);

        // 设置文字颜色
        if (activity.isCharacterSelected(character)) {
            // 选中状态
            textView.setBackgroundResource(android.R.color.darker_gray);
            textView.setTextColor(getContext().getResources().getColor(android.R.color.white));
        } else {
            // 未选中状态
            textView.setBackgroundResource(0);
            if (library.isMarked(character)) {
                textView.setTextColor(getContext().getResources().getColor(android.R.color.holo_green_dark));
            } else {
                textView.setTextColor(getContext().getResources().getColor(android.R.color.black));
            }
        }
        return textView;
    }

    public void updateData(java.util.List<String> newData) {
        clear();
        addAll(newData);
        notifyDataSetChanged();
    }
} 