package com.otfiles.wenyue;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private Spinner themeSelector;
    private Spinner encodingSelector;
    private Switch confirmOperations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 应用当前主题
        applyTheme();
        
        setContentView(R.layout.activity_settings);
        
        // 初始化视图
        initViews();
        
        // 加载保存的设置
        loadSettings();
    }

    private void applyTheme() {
        preferences = getSharedPreferences("app_settings", MODE_PRIVATE);
        String theme = preferences.getString("color_theme", "blue");
        
        if ("green".equals(theme)) {
            setTheme(R.style.AppTheme_Green);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void initViews() {
        // 初始化主题选择器
        themeSelector = findViewById(R.id.theme_selector);
        ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(this,
                R.array.theme_options, android.R.layout.simple_spinner_item);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSelector.setAdapter(themeAdapter);
        
        // 初始化编码选择器
        encodingSelector = findViewById(R.id.encoding_selector);
        ArrayAdapter<CharSequence> encodingAdapter = ArrayAdapter.createFromResource(this,
                R.array.encoding_options, android.R.layout.simple_spinner_item);
        encodingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        encodingSelector.setAdapter(encodingAdapter);
        
        // 初始化开关
        confirmOperations = findViewById(R.id.confirm_operations);
        
        // 设置监听器
        setListeners();
    }

    private void setListeners() {
        themeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTheme = parent.getItemAtPosition(position).toString();
                String themeValue = "blue";
                
                if (selectedTheme.equals("绿色主题")) {
                    themeValue = "green";
                }
                
                // 保存主题设置
                preferences.edit().putString("color_theme", themeValue).apply();
                
                // 提示用户重启应用以应用主题
                Toast.makeText(SettingsActivity.this, 
                        "主题已更改，重启应用后生效", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做任何操作
            }
        });
        
        encodingSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedEncoding = parent.getItemAtPosition(position).toString();
                preferences.edit().putString("default_encoding", selectedEncoding).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做任何操作
            }
        });
        
        confirmOperations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = confirmOperations.isChecked();
                preferences.edit().putBoolean("confirm_operations", isChecked).apply();
            }
        });
    }

    private void loadSettings() {
        // 加载主题设置
        String theme = preferences.getString("color_theme", "blue");
        if ("green".equals(theme)) {
            themeSelector.setSelection(1); // 绿色主题
        } else {
            themeSelector.setSelection(0); // 蓝色主题
        }
        
        // 加载编码设置
        String encoding = preferences.getString("default_encoding", "UTF-8");
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) encodingSelector.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(encoding)) {
                encodingSelector.setSelection(i);
                break;
            }
        }
        
        // 加载操作确认设置
        boolean confirmOps = preferences.getBoolean("confirm_operations", true);
        confirmOperations.setChecked(confirmOps);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存当前状态
        outState.putInt("theme_position", themeSelector.getSelectedItemPosition());
        outState.putInt("encoding_position", encodingSelector.getSelectedItemPosition());
        outState.putBoolean("confirm_operations", confirmOperations.isChecked());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 恢复状态
        if (savedInstanceState != null) {
            themeSelector.setSelection(savedInstanceState.getInt("theme_position", 0));
            encodingSelector.setSelection(savedInstanceState.getInt("encoding_position", 0));
            confirmOperations.setChecked(savedInstanceState.getBoolean("confirm_operations", true));
        }
    }
}