package com.invcolors;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for root operations using su
 */
public class RootUtils {

    private static Boolean rootAvailable = null;

    /**
     * Check if root access is available
     */
    public static boolean isRootAvailable() {
        if (rootAvailable != null) {
            return rootAvailable;
        }
        
        try {
            Process process = Runtime.getRuntime().exec("su -c id");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            rootAvailable = (line != null && line.contains("uid=0"));
            return rootAvailable;
        } catch (Exception e) {
            rootAvailable = false;
            return false;
        }
    }

    /**
     * Execute a command with root privileges
     */
    public static String executeRoot(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get list of installed apps by scanning /data/app/ directory with root
     */
    public static List<AppData> getInstalledAppsWithRoot() {
        List<AppData> apps = new ArrayList<>();
        
        // Scan /data/app/ for user apps
        String userApps = executeRoot("ls -1 /data/app/");
        if (!userApps.isEmpty()) {
            for (String entry : userApps.split("\n")) {
                String packageName = extractPackageName(entry);
                if (packageName != null && !packageName.isEmpty()) {
                    apps.add(new AppData(packageName, packageName, false));
                }
            }
        }
        
        // Scan /data/app/*/ for nested app directories (Android 11+)
        String nestedApps = executeRoot("ls -1d /data/app/*/* 2>/dev/null | grep base.apk | sed 's|/base.apk||' | xargs -I{} basename {}");
        if (!nestedApps.isEmpty()) {
            for (String entry : nestedApps.split("\n")) {
                String packageName = extractPackageName(entry);
                if (packageName != null && !packageName.isEmpty()) {
                    // Check if already added
                    boolean exists = false;
                    for (AppData app : apps) {
                        if (app.packageName.equals(packageName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        apps.add(new AppData(packageName, packageName, false));
                    }
                }
            }
        }
        
        // Alternative: use pm list packages with root
        String pmList = executeRoot("pm list packages -3");
        if (!pmList.isEmpty()) {
            for (String line : pmList.split("\n")) {
                if (line.startsWith("package:")) {
                    String packageName = line.substring(8).trim();
                    if (!packageName.isEmpty()) {
                        boolean exists = false;
                        for (AppData app : apps) {
                            if (app.packageName.equals(packageName)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            apps.add(new AppData(packageName, packageName, false));
                        }
                    }
                }
            }
        }
        
        return apps;
    }

    /**
     * Get apps that are in LSPosed scope for this module
     */
    public static List<String> getLSPosedScopeApps() {
        List<String> scopeApps = new ArrayList<>();
        
        // LSPosed stores scope in /data/adb/lspd/config/modules/
        // The module's scope is stored as a file with package names
        
        // Try common LSPosed config paths
        String[] possiblePaths = {
            "/data/adb/lspd/config/modules/com.invcolors/scope.txt",
            "/data/adb/lspd/config/com.invcolors/scope",
            "/data/adb/lspd/config/modules/com.invcolors",
            "/data/adb/modules/zygisk_lsposed/config/com.invcolors/scope",
        };
        
        for (String path : possiblePaths) {
            String result = executeRoot("cat " + path + " 2>/dev/null");
            if (!result.isEmpty()) {
                for (String line : result.split("\n")) {
                    String pkg = line.trim();
                    if (!pkg.isEmpty() && pkg.contains(".") && !scopeApps.contains(pkg)) {
                        scopeApps.add(pkg);
                    }
                }
            }
        }
        
        // Also try to find scope from LSPosed database
        String dbResult = executeRoot("sqlite3 /data/adb/lspd/config/modules_config.db \"SELECT app_pkg_name FROM scope WHERE module_pkg_name='com.invcolors'\" 2>/dev/null");
        if (!dbResult.isEmpty()) {
            for (String line : dbResult.split("\n")) {
                String pkg = line.trim();
                if (!pkg.isEmpty() && pkg.contains(".") && !scopeApps.contains(pkg)) {
                    scopeApps.add(pkg);
                }
            }
        }
        
        return scopeApps;
    }

    /**
     * Extract package name from directory name (removes version suffix)
     */
    private static String extractPackageName(String dirName) {
        if (dirName == null || dirName.isEmpty()) return null;
        
        // Remove version suffix like "-1", "-2", etc.
        String name = dirName.trim();
        if (name.matches(".*-\\d+$")) {
            name = name.replaceAll("-\\d+$", "");
        }
        
        // Validate package name format
        if (name.contains(".") && !name.startsWith(".") && !name.endsWith(".")) {
            return name;
        }
        
        return null;
    }

    /**
     * Get APK path for a package
     */
    public static String getApkPath(String packageName) {
        String result = executeRoot("pm path " + packageName);
        if (result.startsWith("package:")) {
            return result.substring(8).trim();
        }
        return null;
    }

    /**
     * Extract colors from an APK's resources.arsc
     */
    public static List<Integer> extractColorsFromApk(String packageName) {
        List<Integer> colors = new ArrayList<>();
        
        String apkPath = getApkPath(packageName);
        if (apkPath == null) return colors;
        
        // Use aapt to dump colors from APK
        // First try to find color resources
        String colorDump = executeRoot("aapt dump resources " + apkPath + " 2>/dev/null | grep -E 'color|Color' | head -50");
        
        // Parse hex color values
        if (!colorDump.isEmpty()) {
            String[] lines = colorDump.split("\n");
            for (String line : lines) {
                // Look for hex color patterns like #RRGGBB or #AARRGGBB
                if (line.contains("#")) {
                    int idx = line.indexOf("#");
                    if (idx >= 0 && idx + 7 <= line.length()) {
                        try {
                            String hexPart = line.substring(idx + 1);
                            // Extract 6 or 8 hex digits
                            StringBuilder hex = new StringBuilder();
                            for (char c : hexPart.toCharArray()) {
                                if (Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                                    hex.append(c);
                                    if (hex.length() >= 8) break;
                                } else {
                                    break;
                                }
                            }
                            
                            if (hex.length() >= 6) {
                                int color;
                                if (hex.length() == 6) {
                                    color = 0xFF000000 | Integer.parseInt(hex.toString(), 16);
                                } else {
                                    color = (int) Long.parseLong(hex.toString(), 16);
                                }
                                
                                // Add if not already present
                                if (!colors.contains(color)) {
                                    colors.add(color);
                                }
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid colors
                        }
                    }
                }
            }
        }
        
        // Limit to 20 colors
        if (colors.size() > 20) {
            return colors.subList(0, 20);
        }
        
        return colors;
    }

    /**
     * Data class for app information
     */
    public static class AppData {
        public String packageName;
        public String appName;
        public boolean isHooked;

        public AppData(String packageName, String appName, boolean isHooked) {
            this.packageName = packageName;
            this.appName = appName;
            this.isHooked = isHooked;
        }
    }
}
