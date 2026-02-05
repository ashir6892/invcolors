package com.invcolors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;
import java.io.File;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private LinearLayout appsListLayout;
    private Set<String> hookedApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("invcolors_settings", Context.MODE_PRIVATE);

        // Main layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 20, 20, 20);
        mainLayout.setBackgroundColor(0xFF1E1E1E);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("InvColors - Custom Color Mapping");
        titleView.setTextSize(22);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setPadding(0, 0, 0, 30);
        mainLayout.addView(titleView);

        // Info text
        TextView infoView = new TextView(this);
        infoView.setText("Configure custom colors for each app");
        infoView.setTextSize(14);
        infoView.setTextColor(0xFFAAAAAA);
        infoView.setPadding(0, 0, 0, 20);
        mainLayout.addView(infoView);

        // Buttons row
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button logsButton = new Button(this);
        logsButton.setText("View Logs");
        logsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, LogActivity.class));
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        buttonParams.setMargins(0, 0, 10, 0);
        logsButton.setLayoutParams(buttonParams);
        buttonsLayout.addView(logsButton);

        Button refreshButton = new Button(this);
        refreshButton.setText("Refresh Apps");
        refreshButton.setOnClickListener(v -> loadHookedApps());
        refreshButton.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        buttonsLayout.addView(refreshButton);

        mainLayout.addView(buttonsLayout);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(0xFF444444);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 2);
        dividerParams.setMargins(0, 20, 0, 20);
        divider.setLayoutParams(dividerParams);
        mainLayout.addView(divider);

        // Apps list title
        TextView appsTitle = new TextView(this);
        appsTitle.setText("Hooked Apps:");
        appsTitle.setTextSize(18);
        appsTitle.setTextColor(0xFFFFFFFF);
        appsTitle.setPadding(0, 0, 0, 10);
        mainLayout.addView(appsTitle);

        // Scroll view for apps
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);

        appsListLayout = new LinearLayout(this);
        appsListLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(appsListLayout);
        mainLayout.addView(scrollView);

        setContentView(mainLayout);
        loadHookedApps();
    }

    private void loadHookedApps() {
        appsListLayout.removeAllViews();
        hookedApps = new HashSet<>();
        
        // Read hooked apps from file system
        File hookedAppsDir = new File("/data/data/com.invcolors/hooked_apps/");
        if (hookedAppsDir.exists() && hookedAppsDir.isDirectory()) {
            File[] files = hookedAppsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    hookedApps.add(file.getName());
                }
            }
        }

        if (hookedApps.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No apps hooked yet.\n\n" +
                            "Enable module in LSPosed,\n" +
                            "select target apps,\n" +
                            "and relaunch them.");
            emptyView.setTextColor(0xFFAAAAAA);
            emptyView.setTextSize(14);
            emptyView.setPadding(10, 20, 10, 20);
            appsListLayout.addView(emptyView);
        } else {
            for (String packageName : hookedApps) {
                addAppCard(packageName);
            }
        }
    }

    private void addAppCard(String packageName) {
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundColor(0xFF2D2D2D);
        cardLayout.setPadding(15, 15, 15, 15);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 10);
        cardLayout.setLayoutParams(cardParams);

        // Package name
        TextView packageView = new TextView(this);
        packageView.setText(packageName);
        packageView.setTextColor(0xFFFFFFFF);
        packageView.setTextSize(16);
        packageView.setPadding(0, 0, 0, 10);
        cardLayout.addView(packageView);

        // Settings button
        Button settingsButton = new Button(this);
        settingsButton.setText("Configure Colors");
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ColorSettingsActivity.class);
            intent.putExtra("package_name", packageName);
            startActivity(intent);
        });
        cardLayout.addView(settingsButton);

        appsListLayout.addView(cardLayout);
    }
}
