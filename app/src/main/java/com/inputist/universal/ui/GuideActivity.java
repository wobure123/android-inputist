package com.inputist.universal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.inputist.universal.R;
import com.inputist.universal.ui.adapter.GuideStepAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用指南界面
 */
public class GuideActivity extends AppCompatActivity {
    
    private RecyclerView recyclerGuideSteps;
    private MaterialButton btnOpenSettings;
    private MaterialButton btnGotIt;
    
    private GuideStepAdapter guideAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        
        setupToolbar();
        initializeViews();
        setupGuideSteps();
    }

    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.guide_title));
        }
    }

    private void initializeViews() {
        recyclerGuideSteps = findViewById(R.id.recycler_guide_steps);
        btnOpenSettings = findViewById(R.id.btn_open_settings);
        btnGotIt = findViewById(R.id.btn_got_it);
        
        btnOpenSettings.setOnClickListener(v -> openIMESettings());
        btnGotIt.setOnClickListener(v -> finish());
        
        // 设置RecyclerView
        guideAdapter = new GuideStepAdapter();
        recyclerGuideSteps.setLayoutManager(new LinearLayoutManager(this));
        recyclerGuideSteps.setAdapter(guideAdapter);
    }

    private void setupGuideSteps() {
        List<GuideStep> steps = new ArrayList<>();
        
        steps.add(new GuideStep(
                "1", 
                getString(R.string.guide_step_1_title),
                getString(R.string.guide_step_1_desc),
                "⚙️"
        ));
        
        steps.add(new GuideStep(
                "2", 
                getString(R.string.guide_step_2_title),
                getString(R.string.guide_step_2_desc),
                "🔑"
        ));
        
        steps.add(new GuideStep(
                "3", 
                getString(R.string.guide_step_3_title),
                getString(R.string.guide_step_3_desc),
                "🎯"
        ));
        
        steps.add(new GuideStep(
                "4", 
                getString(R.string.guide_step_4_title),
                getString(R.string.guide_step_4_desc),
                "🚀"
        ));
        
        guideAdapter.setSteps(steps);
    }

    private void openIMESettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开输入法设置", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 指南步骤数据类
     */
    public static class GuideStep {
        private String stepNumber;
        private String title;
        private String description;
        private String emoji;

        public GuideStep(String stepNumber, String title, String description, String emoji) {
            this.stepNumber = stepNumber;
            this.title = title;
            this.description = description;
            this.emoji = emoji;
        }

        public String getStepNumber() {
            return stepNumber;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getEmoji() {
            return emoji;
        }
    }
}
