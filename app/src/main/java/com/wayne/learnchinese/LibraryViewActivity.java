package com.wayne.learnchinese;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public static final String EXTRA_INITIAL_CHARACTER = "initial_character";
    private static final String DELETE_PASSWORD = "123";
    private String highlightedCharacter;

    public boolean isCharacterSelected(String character) {
        return selectedCharacters.contains(character);
    }

    public boolean isBatchMode() {
        return isBatchMode;
    }

    public boolean isHighlightedCharacter(String character) {
        return highlightedCharacter != null && highlightedCharacter.equals(character);
    }

    public boolean isCharacterMarked(String character) {
        return library.isMarked(character);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_view);

        highlightedCharacter = getIntent().getStringExtra(EXTRA_INITIAL_CHARACTER);
        library = new CharacterLibrary(this);
        statsText = findViewById(R.id.statsText);
        gridView = findViewById(R.id.characterGrid);
        batchActionContainer = findViewById(R.id.batchActionContainer);
        MaterialButton toggleBatchModeButton = findViewById(R.id.toggleBatchModeButton);
        MaterialButton markSelectedButton = findViewById(R.id.markSelectedButton);
        MaterialButton unmarkSelectedButton = findViewById(R.id.unmarkSelectedButton);
        MaterialButton deleteSelectedButton = findViewById(R.id.deleteSelectedButton);

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
        FloatingActionButton scrollToTopButton = findViewById(R.id.scrollToTopButton);
        scrollToTopButton.setOnClickListener(v -> gridView.setSelection(0));

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

        deleteSelectedButton.setOnClickListener(v -> {
            if (selectedCharacters.isEmpty()) {
                Toast.makeText(this, "请先选择要删除的汉字", Toast.LENGTH_SHORT).show();
                return;
            }
            showDeletePasswordDialog(
                    "删除选中汉字",
                    "确定要删除已选中的 " + selectedCharacters.size() + " 个汉字吗？",
                    this::deleteSelectedCharacters);
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
            showDeletePasswordDialog(
                    "删除汉字",
                    "确定要删除「" + character + "」吗？",
                    () -> deleteSingleCharacter(character));
            return true;
        });

        scrollToInitialCharacter();
    }

    private void scrollToInitialCharacter() {
        if (highlightedCharacter == null || highlightedCharacter.isEmpty()) {
            return;
        }
        gridView.post(() -> {
            int position = adapter.getPosition(highlightedCharacter);
            if (position < 0) {
                return;
            }
            int numColumns = Math.max(1, gridView.getNumColumns());
            int visibleRows = estimateVisibleRows();
            int targetPosition = Math.max(0, position - (visibleRows / 2) * numColumns);
            gridView.setSelection(targetPosition);
        });
    }

    private int estimateVisibleRows() {
        int rowHeight = 0;
        if (gridView.getChildCount() > 0) {
            View child = gridView.getChildAt(0);
            rowHeight = child.getHeight() + gridView.getVerticalSpacing();
        }
        if (rowHeight <= 0) {
            rowHeight = (int) (44 * getResources().getDisplayMetrics().density);
        }
        return Math.max(1, gridView.getHeight() / Math.max(1, rowHeight));
    }

    private void showDeletePasswordDialog(String title, String message, Runnable deleteAction) {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("请输入删除密码");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message + "\n\n输入密码后才能删除。")
                .setView(container)
                .setPositiveButton("删除", null)
                .setNegativeButton("取消", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String password = input.getText() == null ? "" : input.getText().toString();
                    if (DELETE_PASSWORD.equals(password)) {
                        dialog.dismiss();
                        deleteAction.run();
                    } else {
                        input.setError("密码错误");
                    }
                }));
        dialog.show();
    }

    private void deleteSingleCharacter(String character) {
        if (library.deleteCharacter(character)) {
            if (character.equals(highlightedCharacter)) {
                highlightedCharacter = null;
            }
            selectedCharacters.remove(character);
            adapter.updateData(library.getAllCharacters());
            updateStats();
            Toast.makeText(this, "已删除：" + character, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSelectedCharacters() {
        List<String> charactersToDelete = new ArrayList<>(selectedCharacters);
        int deletedCount = 0;
        for (String character : charactersToDelete) {
            if (library.deleteCharacter(character)) {
                deletedCount++;
                if (character.equals(highlightedCharacter)) {
                    highlightedCharacter = null;
                }
            }
        }
        selectedCharacters.clear();
        adapter.updateData(library.getAllCharacters());
        updateStats();
        Toast.makeText(this, "已删除 " + deletedCount + " 个汉字", Toast.LENGTH_SHORT).show();
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
    private LibraryViewActivity activity;

    public CharacterAdapter(LibraryViewActivity context, java.util.List<String> characters) {
        super(context, R.layout.grid_item_character, characters);
        this.activity = context;
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

        boolean highlighted = activity.isHighlightedCharacter(character);
        boolean selected = activity.isCharacterSelected(character);
        float density = getContext().getResources().getDisplayMetrics().density;
        GradientDrawable background = new GradientDrawable();
        background.setColor(selected ? Color.DKGRAY : Color.TRANSPARENT);
        background.setCornerRadius(4 * density);
        if (highlighted) {
            background.setStroke((int) (3 * density), Color.rgb(255, 152, 0));
        }
        textView.setBackground(background);

        if (selected) {
            textView.setTextColor(getContext().getResources().getColor(android.R.color.white));
        } else {
            if (activity.isCharacterMarked(character)) {
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
