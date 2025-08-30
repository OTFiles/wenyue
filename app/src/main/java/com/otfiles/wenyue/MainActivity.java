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
import android.util.Log;
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

    private static final String TAG = "MainActivity"; // 日志标签
    
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
        
        Log.d(TAG, "onCreate: 活动创建");

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // 初始化UI组件
        ImageButton settingsButton = findViewById(R.id.settings_button);
        favoritesList = findViewById(R.id.favorites_list);
        FloatingActionButton previewFab = findViewById(R.id.preview_fab);
        
        // 加载收藏列表
        loadFavorites();
        Log.d(TAG, "加载收藏列表，共 " + favoritePaths.size() + " 个项目");
        
        // 设置RecyclerView
        adapter = new FileAdapter(this, favoritePaths, true);
        favoritesList.setLayoutManager(new LinearLayoutManager(this));
        favoritesList.setAdapter(adapter);
        Log.d(TAG, "RecyclerView初始化完成");
        
        // 设置按钮点击监听器
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "设置按钮被点击");
                openSettings();
            }
        });
        
        previewFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "预览FAB被点击");
                openDirectoryBrowser();
            }
        });
        
        // 设置适配器点击监听器
        adapter.setOnItemClickListener(new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (position == favoritePaths.size()) {
                    // 点击了"添加收藏"按钮
                    Log.i(TAG, "添加收藏按钮被点击");
                    showAddFavoriteDialog();
                } else {
                    // 点击了收藏项
                    String path = favoritePaths.get(position);
                    Log.i(TAG, "收藏项被点击，位置: " + position + ", 路径: " + path);
                    openPath(path);
                }
            }
            
            @Override
            public void onItemLongClick(View view, int position) {
                if (position < favoritePaths.size()) {
                    // 长按收藏项，显示删除选项
                    Log.i(TAG, "收藏项被长按，位置: " + position);
                    showDeleteDialog(position);
                }
            }
        });
        
        Log.d(TAG, "onCreate完成");
    }
    
    private void loadFavorites() {
        Log.d(TAG, "开始加载收藏列表");
        // 从SharedPreferences加载收藏列表
        Set<String> favoritesSet = preferences.getStringSet(KEY_FAVORITES, new HashSet<String>());
        favoritePaths = new ArrayList<String>(favoritesSet);
        
        // 如果没有收藏项，添加一些示例数据
        if (favoritePaths.isEmpty()) {
            Log.d(TAG, "收藏列表为空，添加示例数据");
            favoritePaths.add("/sdcard/Documents");
            favoritePaths.add("/sdcard/Download/example.txt");
            saveFavorites();
        }
        Log.d(TAG, "收藏列表加载完成，共 " + favoritePaths.size() + " 个项目");
    }
    
    private void saveFavorites() {
        Log.d(TAG, "开始保存收藏列表");
        // 保存收藏列表到SharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        Set<String> favoritesSet = new HashSet<String>(favoritePaths);
        editor.putStringSet(KEY_FAVORITES, favoritesSet);
        boolean success = editor.commit();
        if (success) {
            Log.d(TAG, "收藏列表保存成功");
        } else {
            Log.e(TAG, "收藏列表保存失败");
        }
    }
    
    private void openSettings() {
        Log.i(TAG, "打开设置页面");
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    private void openDirectoryBrowser() {
        Log.i(TAG, "打开目录浏览器，路径: /sdcard");
        Intent intent = new Intent(this, DirectoryActivity.class);
        intent.putExtra("path", "/sdcard");
        startActivity(intent);
    }
    
    private void openPath(String path) {
        if (FileUtils.isDirectory(path)) {
            // 打开目录
            Log.i(TAG, "打开目录: " + path);
            Intent intent = new Intent(this, DirectoryActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
        } else {
            // 打开文件
            Log.i(TAG, "打开文件: " + path);
            Intent intent = new Intent(this, ViewerActivity.class);
            intent.putExtra("path", path);
            startActivity(intent);
        }
    }
    
    private void showAddFavoriteDialog() {
        Log.i(TAG, "显示添加收藏对话框");
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
                Log.i(TAG, "预览按钮被点击");
                // 打开文件选择器
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    Log.d(TAG, "启动文件选择器");
                    startActivityForResult(Intent.createChooser(intent, "选择文件"), 1);
                } catch (android.content.ActivityNotFoundException ex) {
                    Log.e(TAG, "未找到文件管理器", ex);
                    Toast.makeText(MainActivity.this, "未找到文件管理器", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = pathInput.getText().toString().trim();
                Log.i(TAG, "确认按钮被点击，输入路径: " + path);
                
                if (!path.isEmpty() && (FileUtils.fileExists(path) || FileUtils.isDirectory(path))) {
                    if (!favoritePaths.contains(path)) {
                        favoritePaths.add(path);
                        saveFavorites();
                        adapter.updateData(favoritePaths);
                        Log.i(TAG, "成功添加收藏: " + path);
                        Toast.makeText(MainActivity.this, "已添加收藏", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.i(TAG, "路径已在收藏中: " + path);
                        Toast.makeText(MainActivity.this, "该路径已在收藏中", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                } else {
                    Log.w(TAG, "无效路径或文件不存在: " + path);
                    Toast.makeText(MainActivity.this, "路径无效或文件不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        dialog.show();
        Log.d(TAG, "添加收藏对话框已显示");
    }
    
    private void showDeleteDialog(final int position) {
        Log.i(TAG, "显示删除对话框，位置: " + position);
        new AlertDialog.Builder(this)
            .setTitle("删除收藏")
            .setMessage("确定要删除这个收藏吗？")
            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String removedPath = favoritePaths.get(position);
                    favoritePaths.remove(position);
                    saveFavorites();
                    adapter.updateData(favoritePaths);
                    Log.i(TAG, "已删除收藏: " + removedPath);
                    Toast.makeText(MainActivity.this, "已删除收藏", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i(TAG, "取消删除收藏");
                    dialog.dismiss();
                }
            })
            .show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                // 获取选择的文件路径
                String path = FileUtils.getPathFromUri(this, data.getData());
                if (path != null) {
                    Log.i(TAG, "文件选择器返回路径: " + path);
                    // 更新对话框中的路径输入框
                    View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_favorite, null);
                    EditText pathInput = dialogView.findViewById(R.id.path_input);
                    pathInput.setText(path);
                } else {
                    Log.w(TAG, "无法从URI获取路径: " + data.getData());
                }
            } else {
                Log.w(TAG, "文件选择器返回空数据");
            }
        } else if (requestCode == 1 && resultCode == RESULT_CANCELED) {
            Log.i(TAG, "用户取消了文件选择");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: 活动恢复");
        // 重新加载收藏列表，以防在设置中更改了主题
        loadFavorites();
        adapter.updateData(favoritePaths);
        Log.d(TAG, "收藏列表已更新");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: 活动暂停");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: 活动销毁");
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: 活动开始");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: 活动停止");
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: 活动重新启动");
    }
}