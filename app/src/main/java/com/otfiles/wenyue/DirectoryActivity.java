package com.otfiles.wenyue;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.otfiles.wenyue.adapters.DirectoryAdapter;
import com.otfiles.wenyue.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryActivity extends AppCompatActivity implements DirectoryAdapter.OnItemClickListener {

    private TextView titleText;
    private Button viewButton;
    private Button selectAllButton;
    private Button confirmButton;
    private Button moreButton;
    private RecyclerView directoryList;
    
    private DirectoryAdapter adapter;
    private File currentDirectory;
    private boolean isAddFavoriteMode = false;
    
    private static final String STATE_CURRENT_DIRECTORY = "current_directory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyTheme();
        setContentView(R.layout.activity_directory);
        
        initViews();
        setupRecyclerView();
        
        // 获取Intent参数
        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        isAddFavoriteMode = intent.getBooleanExtra("add_favorite", false);
        
        // 恢复状态或初始化目录
        if (savedInstanceState != null) {
            String savedPath = savedInstanceState.getString(STATE_CURRENT_DIRECTORY);
            if (savedPath != null) {
                currentDirectory = new File(savedPath);
            }
        }
        
        if (currentDirectory == null) {
            if (path != null) {
                currentDirectory = new File(path);
            } else {
                currentDirectory = Environment.getExternalStorageDirectory();
            }
        }
        
        loadDirectoryContents();
        updateUI();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentDirectory != null) {
            outState.putString(STATE_CURRENT_DIRECTORY, currentDirectory.getAbsolutePath());
        }
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String theme = prefs.getString("color_theme", "blue");
        
        if ("green".equals(theme)) {
            setTheme(R.style.AppTheme_Green);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void initViews() {
        titleText = findViewById(R.id.title_text);
        viewButton = findViewById(R.id.view_button);
        selectAllButton = findViewById(R.id.select_all_button);
        confirmButton = findViewById(R.id.confirm_button);
        moreButton = findViewById(R.id.more_button);
        directoryList = findViewById(R.id.directory_list);
        
        // 设置按钮点击监听
        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewSelectedFiles();
            }
        });
        
        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.isSelectionMode()) {
                    adapter.clearSelection();
                    selectAllButton.setText(getString(R.string.select_all));
                } else {
                    adapter.selectAll();
                    selectAllButton.setText(getString(R.string.deselect_all));
                }
                updateButtonVisibility();
            }
        });
        
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmFavoriteSelection();
            }
        });
        
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu();
            }
        });
    }

    private void setupRecyclerView() {
        directoryList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DirectoryAdapter(this, new ArrayList<File>(), this);
        directoryList.setAdapter(adapter);
    }

    private void loadDirectoryContents() {
        if (currentDirectory == null || !currentDirectory.exists() || !currentDirectory.isDirectory()) {
            Toast.makeText(this, R.string.invalid_directory, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        titleText.setText(currentDirectory.getAbsolutePath());
        
        File[] filesArray = currentDirectory.listFiles();
        List<File> files = filesArray != null ? Arrays.asList(filesArray) : new ArrayList<File>();
        
        // 排序：文件夹在前，文件在后，按名称排序
        files = FileUtils.sortFiles(files);
        adapter.updateFiles(files);
    }

    private void updateUI() {
        // 更新按钮可见性
        updateButtonVisibility();
        
        // 在添加收藏模式下显示确认按钮
        if (isAddFavoriteMode) {
            confirmButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateButtonVisibility() {
        if (adapter.isSelectionMode()) {
            viewButton.setVisibility(View.VISIBLE);
            selectAllButton.setText(adapter.getSelectedFiles().size() == adapter.getItemCount() ? 
                getString(R.string.deselect_all) : getString(R.string.select_all));
        } else {
            viewButton.setVisibility(View.GONE);
            selectAllButton.setText(getString(R.string.select_all));
        }
    }

    private void viewSelectedFiles() {
        List<File> selectedFiles = adapter.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, R.string.no_files_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 只打开可查看的文件（非目录）
        ArrayList<File> viewableFiles = new ArrayList<>();
        for (File file : selectedFiles) {
            if (!file.isDirectory()) {
                viewableFiles.add(file);
            }
        }
        
        if (viewableFiles.isEmpty()) {
            Toast.makeText(this, R.string.no_viewable_files, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 启动ViewerActivity
        Intent intent = new Intent(this, ViewerActivity.class);
        ArrayList<String> paths = new ArrayList<>();
        for (File file : viewableFiles) {
            paths.add(file.getAbsolutePath());
        }
        intent.putStringArrayListExtra("paths", paths);
        startActivity(intent);
    }

    private void confirmFavoriteSelection() {
        List<File> selectedFiles = adapter.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, R.string.no_files_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 返回选择结果到MainActivity
        Intent resultIntent = new Intent();
        ArrayList<String> paths = new ArrayList<>();
        for (File file : selectedFiles) {
            paths.add(file.getAbsolutePath());
        }
        resultIntent.putStringArrayListExtra("paths", paths);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void showPopupMenu() {
        PopupMenu popup = new PopupMenu(this, moreButton);
        popup.getMenuInflater().inflate(R.menu.directory_more, popup.getMenu());
        
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_cut) {
                    // 剪切功能占位
                    Toast.makeText(DirectoryActivity.this, R.string.cut_function, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.menu_copy) {
                    // 复制功能占位
                    Toast.makeText(DirectoryActivity.this, R.string.copy_function, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.menu_details) {
                    // 详情功能占位
                    Toast.makeText(DirectoryActivity.this, R.string.details_function, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.menu_settings) {
                    // 启动设置
                    startActivity(new Intent(DirectoryActivity.this, SettingsActivity.class));
                    return true;
                }
                return false;
            }
        });
        
        popup.show();
    }

    @Override
    public void onItemClick(File file) {
        if (file.isDirectory()) {
            // 进入子目录
            currentDirectory = file;
            loadDirectoryContents();
            adapter.clearSelection();
            updateButtonVisibility();
        } else {
            // 打开单个文件
            Intent intent = new Intent(this, ViewerActivity.class);
            intent.putExtra("path", file.getAbsolutePath());
            startActivity(intent);
        }
    }

    @Override
    public void onIconClick(File file) {
        adapter.toggleSelection(file);
        updateButtonVisibility();
    }

    @Override
    public void onBackPressed() {
        if (adapter.isSelectionMode()) {
            adapter.clearSelection();
            updateButtonVisibility();
        } else if (currentDirectory != null && !currentDirectory.equals(Environment.getExternalStorageDirectory())) {
            // 返回上级目录
            currentDirectory = currentDirectory.getParentFile();
            loadDirectoryContents();
        } else {
            super.onBackPressed();
        }
    }
}