package com.invcolors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private LinearLayout appsListLayout;
    private PackageManager packageManager;
    private boolean showSystemApps = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("invcolors_settings", Context.MODE_PRIVATE);
        packageManager = getPackageManager();

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
        infoView.setText("All installed apps - configure colors for any app");
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
        buttonParams.setMargins(0, 0, 5, 0);
        logsButton.setLayoutParams(buttonParams);
        buttonsLayout.addView(logsButton);

        Button toggleButton = new Button(this);
        toggleButton.setText("Show All");
        toggleButton.setOnClickListener(v -> {
            showSystemApps = !showSystemApps;
            loadAllApps();
            Toast.makeText(this, showSystemApps ? "Showing all (including services)" : "Showing apps with icons only", 
                         Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        toggleParams.setMargins(5, 0, 0, 0);
        toggleButton.setLayoutParams(toggleParams);
        buttonsLayout.addView(toggleButton);

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
        appsTitle.setText("Installed Apps:");
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
        loadAllApps();
    }

    private void loadAllApps() {
        appsListLayout.removeAllViews();
        
        // Get hooked apps
        Set<String> hookedApps = new HashSet<>();
        File hookedAppsDir = new File("/data/data/com.invcolors/hooked_apps/");
        if (hookedAppsDir.exists() && hookedAppsDir.isDirectory()) {
            File[] files = hookedAppsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    hookedApps.add(file.getName());
                }
            }
        }

        // Get all installed apps
        List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppInfo> appList = new ArrayList<>();
        
        for (ApplicationInfo app : apps) {
            // Check if app has a launcher icon (user-visible apps)
            Intent launchIntent = packageManager.getLaunchIntentForPackage(app.packageName);
            boolean hasLauncherIcon = launchIntent != null;
            
            // When "Toggle System" is OFF:
            // - Show all apps WITH launcher icons (user apps + pre-installed apps)
            // - Hide apps WITHOUT launcher icons (background services, system components)
            // When "Toggle System" is ON:
            // - Show everything
            
            if (!showSystemApps && !hasLauncherIcon && !app.packageName.equals("com.invcolors")) {
                continue; // Skip background services/system components
            }
            
            String appName = app.loadLabel(packageManager).toString();
            boolean isHooked = hookedApps.contains(app.packageName);
            appList.add(new AppInfo(app.packageName, appName, isHooked));
        }
        
        // Sort alphabetically
        Collections.sort(appList, (a, b) -> a.name.compareToIgnoreCase(b.name));
        
        // Display apps
        for (AppInfo appInfo : appList) {
            addAppCard(appInfo);
        }
        
        // Show count
        TextView countView = new TextView(this);
        countView.setText("Total: " + appList.size() + " apps");
        countView.setTextColor(0xFF888888);
        countView.setTextSize(12);
        countView.setPadding(10, 20, 10, 10);
        appsListLayout.addView(countView);
    }

    private void addAppCard(AppInfo appInfo) {
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundColor(appInfo.isHooked ? 0xFF2D4D2D : 0xFF2D2D2D);
        cardLayout.setPadding(15, 15, 15, 15);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 10);
        cardLayout.setLayoutParams(cardParams);

        // App name
        TextView nameView = new TextView(this);
        nameView.setText(appInfo.name + (appInfo.isHooked ? " ✓" : ""));
        nameView.setTextColor(0xFFFFFFFF);
        nameView.setTextSize(16);
        nameView.setPadding(0, 0, 0, 5);
        cardLayout.addView(nameView);

        // Package name
        TextView packageView = new TextView(this);
        packageView.setText(appInfo.packageName);
        packageView.setTextColor(0xFFAAAAAA);
        packageView.setTextSize(12);
        packageView.setPadding(0, 0, 0, 10);
        cardLayout.addView(packageView);

        // Settings button
        Button settingsButton = new Button(this);
        settingsButton.setText("Configure Colors");
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ColorSettingsActivity.class);
            intent.putExtra("package_name", appInfo.packageName);
            startActivity(intent);
        });
        cardLayout.addView(settingsButton);

        appsListLayout.addView(cardLayout);
    }

    private static class AppInfo {
        String packageName;
        String name;
        boolean isHooked;

        AppInfo(String packageName, String name, boolean isHooked) {
            this.packageName = packageName;
            this.name = name;
            this.isHooked = isHooked;
        }
    }
}
