package com.otfiles.wenyue;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.otfiles.wenyue.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ViewerActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView titleText;
    private Spinner encodingSpinner;
    private Button saveButton;
    private ViewPager viewPager;
    private EditText contentEdit;
    
    private List<String> filePaths;
    private List<String> encodings;
    private List<String> contents;
    private int currentPosition;
    private boolean isMultipleFiles;
    
    private static final String[] ENCODING_OPTIONS = {"UTF-8", "GBK", "ISO-8859-1", "GB2312", "Big5"};
    private static final String STATE_CURRENT_POSITION = "currentPosition";
    private static final String STATE_ENCODINGS = "encodings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyTheme();
        setContentView(R.layout.activity_viewer);
        
        initViews();
        setupToolbar();
        getIntentData();
        setupEncodingSpinner();
        setupSaveButton();
        
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            loadFiles();
        }
        
        updateUI();
    }

    private void applyTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String themeColor = prefs.getString("color_theme", "blue");
        
        if ("green".equals(themeColor)) {
            setTheme(R.style.AppTheme_Green);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        titleText = findViewById(R.id.title_text);
        encodingSpinner = findViewById(R.id.encoding_spinner);
        saveButton = findViewById(R.id.save_button);
        viewPager = findViewById(R.id.view_pager);
        contentEdit = findViewById(R.id.content_edit);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent.hasExtra("paths")) {
            filePaths = intent.getStringArrayListExtra("paths");
            isMultipleFiles = filePaths.size() > 1;
        } else if (intent.hasExtra("path")) {
            filePaths = new ArrayList<>();
            filePaths.add(intent.getStringExtra("path"));
            isMultipleFiles = false;
        } else {
            Toast.makeText(this, R.string.error_no_file, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupEncodingSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, ENCODING_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        encodingSpinner.setAdapter(adapter);
        
        encodingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (encodings != null && currentPosition < encodings.size()) {
                    String selectedEncoding = ENCODING_OPTIONS[position];
                    if (!selectedEncoding.equals(encodings.get(currentPosition))) {
                        encodings.set(currentPosition, selectedEncoding);
                        reloadCurrentFile();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentFile();
            }
        });
    }

    private void loadFiles() {
        encodings = new ArrayList<>();
        contents = new ArrayList<>();
        
        for (String path : filePaths) {
            File file = new File(path);
            String encoding = FileUtils.detectEncoding(file);
            String content = FileUtils.readFile(file, encoding);
            
            encodings.add(encoding);
            contents.add(content);
        }
    }

    private void reloadCurrentFile() {
        if (currentPosition < filePaths.size()) {
            String path = filePaths.get(currentPosition);
            String encoding = encodings.get(currentPosition);
            File file = new File(path);
            
            String content = FileUtils.readFile(file, encoding);
            contents.set(currentPosition, content);
            
            if (isMultipleFiles) {
                viewPager.getAdapter().notifyDataSetChanged();
            } else {
                contentEdit.setText(content);
            }
        }
    }

    private void saveCurrentFile() {
        if (currentPosition < filePaths.size()) {
            String path = filePaths.get(currentPosition);
            String encoding = encodings.get(currentPosition);
            String content;
            
            if (isMultipleFiles) {
                ViewFragment fragment = (ViewFragment) ((ViewPagerAdapter) viewPager.getAdapter())
                        .getItem(currentPosition);
                content = fragment.getContent();
            } else {
                content = contentEdit.getText().toString();
            }
            
            boolean success = FileUtils.saveFile(new File(path), content, encoding);
            
            if (success) {
                Toast.makeText(this, R.string.message_save_success, Toast.LENGTH_SHORT).show();
                contents.set(currentPosition, content);
            } else {
                Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUI() {
        if (isMultipleFiles) {
            contentEdit.setVisibility(View.GONE);
            viewPager.setVisibility(View.VISIBLE);
            
            ViewPagerAdapter adapter = new ViewPagerAdapter();
            viewPager.setAdapter(adapter);
            
            viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    currentPosition = position;
                    updateTitle();
                    updateEncodingSpinner();
                }
            });
            
            viewPager.setCurrentItem(currentPosition);
        } else {
            viewPager.setVisibility(View.GONE);
            contentEdit.setVisibility(View.VISIBLE);
            
            if (!contents.isEmpty()) {
                contentEdit.setText(contents.get(0));
            }
        }
        
        updateTitle();
        updateEncodingSpinner();
    }

    private void updateTitle() {
        if (filePaths != null && currentPosition < filePaths.size()) {
            File file = new File(filePaths.get(currentPosition));
            titleText.setText(file.getName());
            
            if (isMultipleFiles) {
                titleText.append(" (" + (currentPosition + 1) + "/" + filePaths.size() + ")");
            }
        }
    }

    private void updateEncodingSpinner() {
        if (encodings != null && currentPosition < encodings.size()) {
            String encoding = encodings.get(currentPosition);
            for (int i = 0; i < ENCODING_OPTIONS.length; i++) {
                if (ENCODING_OPTIONS[i].equals(encoding)) {
                    encodingSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        currentPosition = savedInstanceState.getInt(STATE_CURRENT_POSITION, 0);
        encodings = savedInstanceState.getStringArrayList(STATE_ENCODINGS);
        
        if (encodings == null) {
            loadFiles();
        } else {
            contents = new ArrayList<>();
            for (int i = 0; i < filePaths.size(); i++) {
                File file = new File(filePaths.get(i));
                String content = FileUtils.readFile(file, encodings.get(i));
                contents.add(content);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_POSITION, currentPosition);
        outState.putStringArrayList(STATE_ENCODINGS, new ArrayList<>(encodings));
    }

    private class ViewPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return filePaths.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ViewFragment fragment = new ViewFragment();
            getSupportFragmentManager().beginTransaction().add(container.getId(), fragment).commit();
            
            if (position < contents.size()) {
                fragment.setContent(contents.get(position));
            }
            
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (object instanceof ViewFragment) {
                getSupportFragmentManager().beginTransaction().remove((ViewFragment) object).commit();
            }
        }
    }

    public static class ViewFragment extends android.support.v4.app.Fragment {
        private EditText contentEdit;
        private String content;

        @Override
        public View onCreateView(android.view.LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_view, container, false);
            contentEdit = view.findViewById(R.id.content_edit);
            
            if (content != null) {
                contentEdit.setText(content);
            }
            
            return view;
        }

        public void setContent(String content) {
            this.content = content;
            if (contentEdit != null) {
                contentEdit.setText(content);
            }
        }

        public String getContent() {
            if (contentEdit != null) {
                return contentEdit.getText().toString();
            }
            return content != null ? content : "";
        }
    }
}