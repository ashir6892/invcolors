# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Xposed API classes
-keep class de.robv.android.xposed.** { *; }

# Keep our hook class
-keep class com.invcolors.MainHook { *; }
