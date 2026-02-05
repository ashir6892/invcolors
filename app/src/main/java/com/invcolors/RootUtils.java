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
     * Extract colors from an APK's resources
     * Uses multiple methods since aapt may not be available
     */
    public static List<Integer> extractColorsFromApk(String packageName) {
        List<Integer> colors = new ArrayList<>();
        
        String apkPath = getApkPath(packageName);
        if (apkPath == null) return colors;
        
        // Method 1: Use unzip to extract resources.arsc and parse it
        String tempDir = "/data/local/tmp/invcolors_" + System.currentTimeMillis();
        executeRoot("mkdir -p " + tempDir);
        
        try {
            // Try to extract and search for color hex patterns
            // Method A: Search in manifest and resource files using strings
            String stringsOutput = executeRoot("strings " + apkPath + " 2>/dev/null | grep -oE '#[0-9A-Fa-f]{6,8}' | head -30");
            parseHexColors(stringsOutput, colors);
            
            // Method B: Unzip and grep for colors in XML
            if (colors.size() < 5) {
                executeRoot("cd " + tempDir + " && unzip -q -o " + apkPath + " 'res/values/colors.xml' 2>/dev/null");
                String colorsXml = executeRoot("cat " + tempDir + "/res/values/colors.xml 2>/dev/null");
                parseHexColors(colorsXml, colors);
            }
            
            // Method C: Search for color patterns in any XML
            if (colors.size() < 5) {
                executeRoot("cd " + tempDir + " && unzip -q -o " + apkPath + " 'res/values/*.xml' 2>/dev/null");
                String allXml = executeRoot("grep -roh '#[0-9A-Fa-f]\\{6,8\\}' " + tempDir + "/res/ 2>/dev/null | head -30");
                parseHexColors(allXml, colors);
            }
            
            // Method D: Extract colors from styles
            if (colors.size() < 5) {
                executeRoot("cd " + tempDir + " && unzip -q -o " + apkPath + " 'res/values/styles.xml' 2>/dev/null");
                String stylesXml = executeRoot("cat " + tempDir + "/res/values/styles.xml 2>/dev/null");
                parseHexColors(stylesXml, colors);
            }
            
            // Method E: Binary grep on APK for color patterns (last resort)
            if (colors.size() < 3) {
                String hexDump = executeRoot("xxd " + apkPath + " 2>/dev/null | grep -oE '[0-9a-f]{8}' | head -100");
                // Only add common color-like patterns
                if (!hexDump.isEmpty()) {
                    for (String hex : hexDump.split("\\s+")) {
                        hex = hex.trim();
                        if (hex.length() == 8) {
                            try {
                                int val = (int) Long.parseLong(hex, 16);
                                // Only add if it looks like a color (high alpha)
                                int alpha = (val >> 24) & 0xFF;
                                if (alpha > 200 && !colors.contains(val) && colors.size() < 20) {
                                    colors.add(val);
                                }
                            } catch (Exception e) {}
                        }
                    }
                }
            }
            
        } finally {
            // Cleanup
            executeRoot("rm -rf " + tempDir);
        }
        
        // Add common colors if none found
        if (colors.isEmpty()) {
            // Add some common colors as suggestions
            colors.add(0xFFFFFFFF); // White
            colors.add(0xFF000000); // Black
            colors.add(0xFFF5F5F5); // Light gray
            colors.add(0xFF212121); // Dark gray
            colors.add(0xFF1976D2); // Blue
            colors.add(0xFFD32F2F); // Red
        }
        
        // Limit to 20 colors
        if (colors.size() > 20) {
            return colors.subList(0, 20);
        }
        
        return colors;
    }
    
    /**
     * Parse hex color values from text
     */
    private static void parseHexColors(String text, List<Integer> colors) {
        if (text == null || text.isEmpty()) return;
        
        String[] lines = text.split("\\n");
        for (String line : lines) {
            int idx = line.indexOf("#");
            while (idx >= 0 && idx + 4 < line.length()) {
                try {
                    StringBuilder hex = new StringBuilder();
                    for (int i = idx + 1; i < line.length() && hex.length() < 8; i++) {
                        char c = line.charAt(i);
                        if (Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
                            hex.append(c);
                        } else {
                            break;
                        }
                    }
                    
                    if (hex.length() >= 6) {
                        int color;
                        if (hex.length() == 6) {
                            color = 0xFF000000 | Integer.parseInt(hex.toString(), 16);
                        } else if (hex.length() == 8) {
                            color = (int) Long.parseLong(hex.toString(), 16);
                        } else {
                            color = 0xFF000000 | Integer.parseInt(hex.substring(0, 6), 16);
                        }
                        
                        if (!colors.contains(color) && colors.size() < 25) {
                            colors.add(color);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid
                }
                
                idx = line.indexOf("#", idx + 1);
            }
        }
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
