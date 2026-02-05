package com.invcolors;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ColorSettingsActivity extends Activity {

    private SharedPreferences prefs;
    private String packageName;
    
    private int sourceColor = Color.WHITE; // Default: white
    private int targetColor = Color.BLACK; // Default: black
    
    private android.view.View sourceColorPreview;
    private android.view.View targetColorPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        packageName = getIntent().getStringExtra("package_name");
        prefs = getSharedPreferences("invcolors_settings", Context.MODE_WORLD_READABLE);
        
        // Load saved colors
        sourceColor = prefs.getInt(packageName + "_source", Color.WHITE);
        targetColor = prefs.getInt(packageName + "_target", Color.BLACK);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 20, 20, 20);
        mainLayout.setBackgroundColor(0xFF1E1E1E);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("Color Settings");
        titleView.setTextSize(22);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setPadding(0, 0, 0, 10);
        mainLayout.addView(titleView);

        // Package name
        TextView packageView = new TextView(this);
        packageView.setText(packageName);
        packageView.setTextSize(14);
        packageView.setTextColor(0xFFAAAAAA);
        packageView.setPadding(0, 0, 0, 30);
        mainLayout.addView(packageView);

        // Source color section
        TextView sourceLabel = new TextView(this);
        sourceLabel.setText("Color to Replace:");
        sourceLabel.setTextSize(16);
        sourceLabel.setTextColor(0xFFFFFFFF);
        sourceLabel.setPadding(0, 0, 0, 10);
        mainLayout.addView(sourceLabel);

        sourceColorPreview = new android.view.View(this);
        sourceColorPreview.setBackgroundColor(sourceColor);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 100);
        previewParams.setMargins(0, 0, 0, 10);
        sourceColorPreview.setLayoutParams(previewParams);
        mainLayout.addView(sourceColorPreview);

        // Source color buttons
        LinearLayout sourceButtonsLayout = createColorButtons(true);
        mainLayout.addView(sourceButtonsLayout);

        // Divider
        android.view.View divider = new android.view.View(this);
        divider.setBackgroundColor(0xFF444444);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 2);
        dividerParams.setMargins(0, 30, 0, 30);
        divider.setLayoutParams(dividerParams);
        mainLayout.addView(divider);

        // Target color section
        TextView targetLabel = new TextView(this);
        targetLabel.setText("Replace With:");
        targetLabel.setTextSize(16);
        targetLabel.setTextColor(0xFFFFFFFF);
        targetLabel.setPadding(0, 0, 0, 10);
        mainLayout.addView(targetLabel);

        targetColorPreview = new android.view.View(this);
        targetColorPreview.setBackgroundColor(targetColor);
        targetColorPreview.setLayoutParams(previewParams);
        mainLayout.addView(targetColorPreview);

        // Target color buttons
        LinearLayout targetButtonsLayout = createColorButtons(false);
        mainLayout.addView(targetButtonsLayout);

        // Save button
        Button saveButton = new Button(this);
        saveButton.setText("Save Settings");
        saveButton.setTextSize(18);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        saveParams.setMargins(0, 30, 0, 0);
        saveButton.setLayoutParams(saveParams);
        saveButton.setOnClickListener(v -> {
            saveSettings();
        });
        mainLayout.addView(saveButton);

        setContentView(mainLayout);
    }

    private LinearLayout createColorButtons(boolean isSource) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 0, 0, 20);

        // Row 1: White, Light Gray, Gray, Dark Gray, Black
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        addColorButton(row1, "White", Color.WHITE, isSource);
        addColorButton(row1, "Light", 0xFFCCCCCC, isSource);
        addColorButton(row1, "Gray", Color.GRAY, isSource);
        addColorButton(row1, "Dark", 0xFF444444, isSource);
        addColorButton(row1, "Black", Color.BLACK, isSource);
        layout.addView(row1);

        // Row 2: Red, Green, Blue, Yellow, Cyan
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        addColorButton(row2, "Red", Color.RED, isSource);
        addColorButton(row2, "Green", Color.GREEN, isSource);
        addColorButton(row2, "Blue", Color.BLUE, isSource);
        addColorButton(row2, "Yellow", Color.YELLOW, isSource);
        addColorButton(row2, "Cyan", Color.CYAN, isSource);
        layout.addView(row2);

        return layout;
    }

    private void addColorButton(LinearLayout parent, String label, int color, boolean isSource) {
        Button button = new Button(this);
        button.setText(label);
        button.setBackgroundColor(color);
        
        // Set text color based on background
        int textColor = (color == Color.BLACK || color == Color.BLUE || color == Color.RED || color == 0xFF444444) 
            ? Color.WHITE : Color.BLACK;
        button.setTextColor(textColor);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        params.setMargins(2, 5, 2, 5);
        button.setLayoutParams(params);
        
        button.setOnClickListener(v -> {
            if (isSource) {
                sourceColor = color;
                sourceColorPreview.setBackgroundColor(color);
            } else {
                targetColor = color;
                targetColorPreview.setBackgroundColor(color);
            }
        });
        
        parent.addView(button);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(packageName + "_source", sourceColor);
        editor.putInt(packageName + "_target", targetColor);
        editor.apply();
        
        Toast.makeText(this, "Colors saved! Restart app to apply changes.", Toast.LENGTH_LONG).show();
        finish();
    }
}
