package com.invcolors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private LinearLayout appsListLayout;
    private PackageManager packageManager;
    private TextView statusView;
    private ProgressBar progressBar;
    private EditText searchBox;
    private List<AppInfo> allApps = new ArrayList<>();
    private Set<String> pinnedApps = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("invcolors_settings", Context.MODE_PRIVATE);
        packageManager = getPackageManager();
        
        // Load pinned apps
        pinnedApps = new HashSet<>(prefs.getStringSet("pinned_apps", new HashSet<>()));

        // Main layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 20, 20, 20);
        mainLayout.setBackgroundColor(0xFF1E1E1E);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("InvColors - Color Mapping");
        titleView.setTextSize(20);
        titleView.setTextColor(0xFFFFFFFF);
        titleView.setPadding(0, 0, 0, 15);
        mainLayout.addView(titleView);

        // Search box
        searchBox = new EditText(this);
        searchBox.setHint("Search apps...");
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setHintTextColor(0xFF888888);
        searchBox.setBackgroundColor(0xFF333333);
        searchBox.setPadding(20, 15, 20, 15);
        searchBox.setSingleLine(true);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, 0, 0, 15);
        searchBox.setLayoutParams(searchParams);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        mainLayout.addView(searchBox);

        // Status and buttons row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        
        statusView = new TextView(this);
        statusView.setText("Loading...");
        statusView.setTextSize(12);
        statusView.setTextColor(0xFFAAAAAA);
        statusView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        topRow.addView(statusView);

        Button logsButton = new Button(this);
        logsButton.setText("Logs");
        logsButton.setTextSize(11);
        logsButton.setPadding(15, 5, 15, 5);
        logsButton.setOnClickListener(v -> startActivity(new Intent(this, LogActivity.class)));
        topRow.addView(logsButton);

        mainLayout.addView(topRow);

        // Progress bar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 8);
        progressParams.setMargins(0, 10, 0, 10);
        progressBar.setLayoutParams(progressParams);
        mainLayout.addView(progressBar);

        // Scroll view for apps
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);

        appsListLayout = new LinearLayout(this);
        appsListLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(appsListLayout);
        mainLayout.addView(scrollView);

        // Hint text
        TextView hintView = new TextView(this);
        hintView.setText("💡 Long-press app to pin to top");
        hintView.setTextSize(11);
        hintView.setTextColor(0xFF888888);
        hintView.setPadding(0, 10, 0, 0);
        mainLayout.addView(hintView);

        setContentView(mainLayout);
        loadAppsAsync();
    }

    private void loadAppsAsync() {
        progressBar.setVisibility(View.VISIBLE);
        statusView.setText("Loading apps...");
        appsListLayout.removeAllViews();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<AppInfo> apps = new ArrayList<>();
            
            List<AppInfo> apps = new ArrayList<>();
            Set<String> addedPackages = new HashSet<>();
            
            // 1. Get apps using Root (su) - Critical for bypassing BetterKnownInstalled
            if (RootUtils.isRootAvailable()) {
                List<RootUtils.AppData> rootApps = RootUtils.getInstalledAppsWithRoot();
                for (RootUtils.AppData app : rootApps) {
                    if (!addedPackages.contains(app.packageName)) {
                        String name = app.appName;
                        // Try to get better label from PM even if list hidden
                        try {
                            ApplicationInfo info = packageManager.getApplicationInfo(app.packageName, 0);
                            name = info.loadLabel(packageManager).toString();
                        } catch (Exception e) {
                            // Keep package name/folder name if PM lookup fails
                        }
                        
                        boolean isPinned = pinnedApps.contains(app.packageName);
                        apps.add(new AppInfo(app.packageName, name, isPinned));
                        addedPackages.add(app.packageName);
                    }
                }
            }
            
            // 2. Get apps using PackageManager (for standard apps + better labels)
            try {
                List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                for (ApplicationInfo app : installedApps) {
                    Intent launchIntent = packageManager.getLaunchIntentForPackage(app.packageName);
                    if (launchIntent != null) {
                        if (!addedPackages.contains(app.packageName)) {
                             String appName = app.loadLabel(packageManager).toString();
                             boolean isPinned = pinnedApps.contains(app.packageName);
                             apps.add(new AppInfo(app.packageName, appName, isPinned));
                             addedPackages.add(app.packageName);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore PM errors if root worked
            }
            
            // Sort: pinned first, then alphabetically
            Collections.sort(apps, (a, b) -> {
                if (a.isPinned && !b.isPinned) return -1;
                if (!a.isPinned && b.isPinned) return 1;
                return a.name.compareToIgnoreCase(b.name);
            });
            
            allApps = apps;
            
            new Handler(Looper.getMainLooper()).post(() -> {
                progressBar.setVisibility(View.GONE);
                statusView.setText(apps.size() + " apps | " + pinnedApps.size() + " pinned");
                displayApps(apps);
            });
        });
    }

    private void filterApps(String query) {
        if (query.isEmpty()) {
            displayApps(allApps);
            return;
        }
        
        String lowerQuery = query.toLowerCase();
        List<AppInfo> filtered = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (app.name.toLowerCase().contains(lowerQuery) || 
                app.packageName.toLowerCase().contains(lowerQuery)) {
                filtered.add(app);
            }
        }
        displayApps(filtered);
    }

    private void displayApps(List<AppInfo> apps) {
        appsListLayout.removeAllViews();
        
        if (apps.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No apps found");
            emptyView.setTextColor(0xFFAAAAAA);
            emptyView.setPadding(10, 20, 10, 20);
            appsListLayout.addView(emptyView);
            return;
        }
        
        for (AppInfo appInfo : apps) {
            addAppCard(appInfo);
        }
    }

    private void addAppCard(AppInfo appInfo) {
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setBackgroundColor(appInfo.isPinned ? 0xFF3D3D1D : 0xFF2D2D2D);
        cardLayout.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 6);
        cardLayout.setLayoutParams(cardParams);

        // Long press to pin/unpin
        cardLayout.setOnLongClickListener(v -> {
            togglePin(appInfo);
            return true;
        });

        // App info column
        LinearLayout infoColumn = new LinearLayout(this);
        infoColumn.setOrientation(LinearLayout.VERTICAL);
        infoColumn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // App name with pin indicator
        TextView nameView = new TextView(this);
        nameView.setText((appInfo.isPinned ? "📌 " : "") + appInfo.name);
        nameView.setTextColor(0xFFFFFFFF);
        nameView.setTextSize(14);
        infoColumn.addView(nameView);

        // Package name
        TextView packageView = new TextView(this);
        packageView.setText(appInfo.packageName);
        packageView.setTextColor(0xFF888888);
        packageView.setTextSize(10);
        infoColumn.addView(packageView);

        cardLayout.addView(infoColumn);

        // Colors button
        Button colorsButton = new Button(this);
        colorsButton.setText("Colors");
        colorsButton.setTextSize(11);
        colorsButton.setPadding(15, 5, 15, 5);
        colorsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ColorSettingsActivity.class);
            intent.putExtra("package_name", appInfo.packageName);
            startActivity(intent);
        });
        cardLayout.addView(colorsButton);

        appsListLayout.addView(cardLayout);
    }

    private void togglePin(AppInfo appInfo) {
        if (appInfo.isPinned) {
            pinnedApps.remove(appInfo.packageName);
            appInfo.isPinned = false;
            Toast.makeText(this, "Unpinned: " + appInfo.name, Toast.LENGTH_SHORT).show();
        } else {
            pinnedApps.add(appInfo.packageName);
            appInfo.isPinned = true;
            Toast.makeText(this, "Pinned: " + appInfo.name, Toast.LENGTH_SHORT).show();
        }
        
        // Save pinned apps
        prefs.edit().putStringSet("pinned_apps", pinnedApps).apply();
        
        // Re-sort and display
        Collections.sort(allApps, (a, b) -> {
            if (a.isPinned && !b.isPinned) return -1;
            if (!a.isPinned && b.isPinned) return 1;
            return a.name.compareToIgnoreCase(b.name);
        });
        
        String query = searchBox.getText().toString();
        if (query.isEmpty()) {
            displayApps(allApps);
        } else {
            filterApps(query);
        }
        
        statusView.setText(allApps.size() + " apps | " + pinnedApps.size() + " pinned");
    }

    private static class AppInfo {
        String packageName;
        String name;
        boolean isPinned;

        AppInfo(String packageName, String name, boolean isPinned) {
            this.packageName = packageName;
            this.name = name;
            this.isPinned = isPinned;
        }
    }
}
