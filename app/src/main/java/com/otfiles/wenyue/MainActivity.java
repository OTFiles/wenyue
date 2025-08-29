package com.otfiles.wenyue;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.otfiles.wenyue.adapters.FileAdapter;
import com.otfiles.wenyue.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private RecyclerView favoritesList;
    private FileAdapter adapter;
    private List<String> favoritePaths;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "FavoritesPrefs";
    private static final String KEY_FAVORITES = "favorites";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // 初始化UI组件
        ImageButton settingsButton = findViewById(R.id.settings_button);
        favoritesList = findViewById(R.id.favorites_list);
        FloatingActionButton previewFab = findViewById(R.id.preview_fab);
        
        // 加载收藏列表
        loadFavorites();
        
        // 设置RecyclerView
        adapter = new FileAdapter(this, favoritePaths, true);
        favoritesList.setLayoutManager(new LinearLayoutManager(this));
        favoritesList.setAdapter(adapter);
        
        // 设置按钮点击监听器
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
        
        previewFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDirectoryBrowser();
            }
        });
        
        // 设置适配器点击监听器
        adapter.setOnItemClickListener(new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (position == favoritePaths.size()) {
                    // 点击了"添加收藏"按钮
                    showAddFavoriteDialog();
                } else {
                    // 点击了收藏项
                    String path = favoritePaths.get(position);
                    openPath(path);
                }
            }
            
            @Override
            public void onItemLongClick(View view, int position) {
                if (position < favoritePaths.size()) {
                    // 长按收藏项，显示删除选项
                    showDeleteDialog(position);
                }
            }
        });
    }
    
    private void loadFavorites() {
        // 从SharedPreferences加载收藏列表
        Set<String> favoritesSet = preferences.getStringSet(KEY_FAVORITES, new HashSet<String>());
        favoritePaths = new ArrayList<String>(favoritesSet);
        
        // 如果没有收藏项，添加一些示例数据
        if (favoritePaths.isEmpty()) {
            favoritePaths.add("/sdcard/Documents");
            favoritePaths.add("/sdcard/Download/example.txt");
            saveFavorites();
        }
    }
    
    private void saveFavorites() {
        // 保存收藏列表到SharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> favoritesSet = new HashSet<String>(favoritePaths);
        editor.putStringSet(KEY_FAVORITES, favoritesSet);
        editor.apply();
    }
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    private void openDirectoryBrowser() {
        Intent intent = new Intent(this, DirectoryActivity.class);
        intent.putExtra("path", "/sdcard");
        startActivity(intent);
    }
    
    private void openPath(String path) {
        if (FileUtils.isDirectory(path)) {
            // 打开目录
            Intent intent = new Intent(this, DirectoryActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
        } else {
            // 打开文件
            Intent intent = new Intent(this, ViewerActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
        }
    }
    
    private void showAddFavoriteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_favorite, null);
        
        final EditText pathInput = dialogView.findViewById(R.id.path_input);
        View previewButton = dialogView.findViewById(R.id.preview_button);
        View confirmButton = dialogView.findViewById(R.id.confirm_button);
        
        final AlertDialog dialog = builder.setView(dialogView).create();
        
        previewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打开文件选择器
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(intent, "选择文件"), 1);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, "未找到文件管理器", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = pathInput.getText().toString().trim();
                if (!path.isEmpty() && (FileUtils.fileExists(path) || FileUtils.isDirectory(path))) {
                    if (!favoritePaths.contains(path)) {
                        favoritePaths.add(path);
                        saveFavorites();
                        adapter.updateData(favoritePaths);
                        Toast.makeText(MainActivity.this, "已添加收藏", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "该路径已在收藏中", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "路径无效或文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        dialog.show();
    }
    
    private void showDeleteDialog(final int position) {
        new AlertDialog.Builder(this)
            .setTitle("删除收藏")
            .setMessage("确定要删除这个收藏吗？")
            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    favoritePaths.remove(position);
                    saveFavorites();
                    adapter.updateData(favoritePaths);
                    Toast.makeText(MainActivity.this, "已删除收藏", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                // 获取选择的文件路径
                String path = FileUtils.getPathFromUri(this, data.getData());
                if (path != null) {
                    // 更新对话框中的路径输入框
                    View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_favorite, null);
                    EditText pathInput = dialogView.findViewById(R.id.path_input);
                    pathInput.setText(path);
                }
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 重新加载收藏列表，以防在设置中更改了主题
        loadFavorites();
        adapter.updateData(favoritePaths);
    }
}