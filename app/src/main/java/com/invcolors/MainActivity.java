package com.invcolors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
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
    private boolean useRoot = true;

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
        titleView.setPadding(0, 0, 0, 20);
        mainLayout.addView(titleView);

        // Status text
        statusView = new TextView(this);
        statusView.setText("Checking root access...");
        statusView.setTextSize(14);
        statusView.setTextColor(0xFFAAAAAA);
        statusView.setPadding(0, 0, 0, 10);
        mainLayout.addView(statusView);

        // Progress bar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 10);
        progressParams.setMargins(0, 0, 0, 20);
        progressBar.setLayoutParams(progressParams);
        mainLayout.addView(progressBar);

        // Buttons row
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button logsButton = new Button(this);
        logsButton.setText("Logs");
        logsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, LogActivity.class));
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        buttonParams.setMargins(0, 0, 5, 0);
        logsButton.setLayoutParams(buttonParams);
        buttonsLayout.addView(logsButton);

        Button refreshButton = new Button(this);
        refreshButton.setText("Refresh");
        refreshButton.setOnClickListener(v -> loadAppsAsync());
        refreshButton.setLayoutParams(buttonParams);
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
        loadAppsAsync();
    }

    private void loadAppsAsync() {
        progressBar.setVisibility(View.VISIBLE);
        statusView.setText("Loading apps...");
        appsListLayout.removeAllViews();

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean rootAvailable = RootUtils.isRootAvailable();
            List<AppInfo> apps = new ArrayList<>();
            Set<String> lsposedApps = new HashSet<>();
            
            if (rootAvailable) {
                // Get LSPosed scope apps
                lsposedApps.addAll(RootUtils.getLSPosedScopeApps());
                
                // Get installed apps using root
                List<RootUtils.AppData> rootApps = RootUtils.getInstalledAppsWithRoot();
                for (RootUtils.AppData app : rootApps) {
                    boolean isInScope = lsposedApps.contains(app.packageName);
                    apps.add(new AppInfo(app.packageName, app.appName, isInScope));
                }
            }
            
            // Also try PackageManager as fallback
            if (apps.size() < 10) {
                try {
                    List<ApplicationInfo> pmApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                    for (ApplicationInfo app : pmApps) {
                        Intent launchIntent = packageManager.getLaunchIntentForPackage(app.packageName);
                        if (launchIntent != null) {
                            String appName = app.loadLabel(packageManager).toString();
                            boolean isInScope = lsposedApps.contains(app.packageName);
                            
                            // Check if already added
                            boolean exists = false;
                            for (AppInfo existing : apps) {
                                if (existing.packageName.equals(app.packageName)) {
                                    exists = true;
                                    // Update name if we have a better one
                                    if (!appName.equals(app.packageName)) {
                                        existing.name = appName;
                                    }
                                    break;
                                }
                            }
                            if (!exists) {
                                apps.add(new AppInfo(app.packageName, appName, isInScope));
                            }
                        }
                    }
                } catch (Exception e) {
                    // PackageManager failed, rely on root results
                }
            }
            
            // Try to resolve app names for root-found apps
            for (AppInfo app : apps) {
                if (app.name.equals(app.packageName)) {
                    try {
                        ApplicationInfo appInfo = packageManager.getApplicationInfo(app.packageName, 0);
                        app.name = appInfo.loadLabel(packageManager).toString();
                    } catch (Exception e) {
                        // Keep package name as name
                    }
                }
            }
            
            // Sort alphabetically
            Collections.sort(apps, (a, b) -> a.name.compareToIgnoreCase(b.name));
            
            final List<AppInfo> finalApps = apps;
            final boolean finalRootAvailable = rootAvailable;
            final int scopeCount = lsposedApps.size();
            
            new Handler(Looper.getMainLooper()).post(() -> {
                progressBar.setVisibility(View.GONE);
                
                String status = finalRootAvailable ? "✓ Root available" : "✗ No root";
                status += " | " + finalApps.size() + " apps";
                if (scopeCount > 0) {
                    status += " | " + scopeCount + " in LSPosed scope";
                }
                statusView.setText(status);
                
                if (finalApps.isEmpty()) {
                    TextView emptyView = new TextView(this);
                    emptyView.setText("No apps found.\n\nGrant root access in KernelSU\nand tap Refresh.");
                    emptyView.setTextColor(0xFFAAAAAA);
                    emptyView.setTextSize(14);
                    emptyView.setPadding(10, 20, 10, 20);
                    appsListLayout.addView(emptyView);
                } else {
                    for (AppInfo appInfo : finalApps) {
                        addAppCard(appInfo);
                    }
                }
            });
        });
    }

    private void addAppCard(AppInfo appInfo) {
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        // Green if in LSPosed scope, gray otherwise
        cardLayout.setBackgroundColor(appInfo.isInScope ? 0xFF2D4D2D : 0xFF2D2D2D);
        cardLayout.setPadding(15, 12, 15, 12);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 8);
        cardLayout.setLayoutParams(cardParams);

        // App name row
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView nameView = new TextView(this);
        nameView.setText(appInfo.name);
        nameView.setTextColor(0xFFFFFFFF);
        nameView.setTextSize(15);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        nameRow.addView(nameView);
        
        if (appInfo.isInScope) {
            TextView scopeBadge = new TextView(this);
            scopeBadge.setText(" LSP ✓");
            scopeBadge.setTextColor(0xFF88FF88);
            scopeBadge.setTextSize(12);
            nameRow.addView(scopeBadge);
        }
        
        cardLayout.addView(nameRow);

        // Package name
        TextView packageView = new TextView(this);
        packageView.setText(appInfo.packageName);
        packageView.setTextColor(0xFF888888);
        packageView.setTextSize(11);
        packageView.setPadding(0, 2, 0, 8);
        cardLayout.addView(packageView);

        // Buttons row
        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);

        // Configure colors button
        Button configButton = new Button(this);
        configButton.setText("Colors");
        configButton.setTextSize(12);
        configButton.setPadding(10, 5, 10, 5);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnParams.setMargins(0, 0, 5, 0);
        configButton.setLayoutParams(btnParams);
        configButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ColorSettingsActivity.class);
            intent.putExtra("package_name", appInfo.packageName);
            startActivity(intent);
        });
        buttonsRow.addView(configButton);

        // Extract colors button
        Button extractButton = new Button(this);
        extractButton.setText("Detect");
        extractButton.setTextSize(12);
        extractButton.setPadding(10, 5, 10, 5);
        extractButton.setLayoutParams(btnParams);
        extractButton.setOnClickListener(v -> {
            Toast.makeText(this, "Extracting colors from " + appInfo.name + "...", Toast.LENGTH_SHORT).show();
            Executors.newSingleThreadExecutor().execute(() -> {
                List<Integer> colors = RootUtils.extractColorsFromApk(appInfo.packageName);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (colors.isEmpty()) {
                        Toast.makeText(this, "No colors found (aapt may not be available)", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Found " + colors.size() + " colors!", Toast.LENGTH_SHORT).show();
                        // Open color settings with detected colors
                        Intent intent = new Intent(this, ColorSettingsActivity.class);
                        intent.putExtra("package_name", appInfo.packageName);
                        int[] colorArray = new int[colors.size()];
                        for (int i = 0; i < colors.size(); i++) {
                            colorArray[i] = colors.get(i);
                        }
                        intent.putExtra("detected_colors", colorArray);
                        startActivity(intent);
                    }
                });
            });
        });
        buttonsRow.addView(extractButton);

        cardLayout.addView(buttonsRow);
        appsListLayout.addView(cardLayout);
    }

    private static class AppInfo {
        String packageName;
        String name;
        boolean isInScope;

        AppInfo(String packageName, String name, boolean isInScope) {
            this.packageName = packageName;
            this.name = name;
            this.isInScope = isInScope;
        }
    }
}
