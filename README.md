# InvColors - LSPosed Color Inversion Module

A simple LSPosed/Xposed module that inverts the colors of Android applications using ColorMatrix transformation.

## Features

- 🎨 Inverts all colors in target applications
- ⚡ Lightweight and efficient
- 🔧 Works with LSPosed framework
- 🛡️ Automatically skips system UI to prevent issues
- 📱 Compatible with Android 7.0+ (API 24+)

## How It Works

The module hooks into the `View.draw()` method of Android views and applies a color inversion filter using a ColorMatrix. This transforms all RGB values while preserving transparency:

```
R' = 255 - R
G' = 255 - G
B' = 255 - B
A' = A (unchanged)
```

## Installation

### Requirements

- Rooted Android device
- LSPosed or EdXposed framework installed
- Android 7.0 (API 24) or higher

### Steps

1. **Build the APK** (or download a pre-built release)
   ```bash
   cd invcolors
   ./gradlew assembleRelease
   ```
   The APK will be located at `app/build/outputs/apk/release/app-release.apk`

2. **Install the APK** on your Android device

3. **Enable in LSPosed**
   - Open LSPosed Manager
   - Go to "Modules" tab
   - Enable "InvColors"
   - Tap on the module to select target applications

4. **Reboot** your device (soft reboot is sufficient)

5. **Launch target apps** - colors should now be inverted!

## Usage

### Selecting Target Apps

1. Open LSPosed Manager
2. Tap on "InvColors" module
3. Select "Scope" tab
4. Choose which apps you want to apply color inversion to
5. Reboot (soft reboot)

### Tips

- **Avoid enabling for system apps** - The module automatically excludes Android system, SystemUI, and itself
- **Performance** - The module is very lightweight and shouldn't impact performance
- **Compatibility** - Works with most apps, but apps using OpenGL/Vulkan rendering may not be affected

## Building from Source

### Prerequisites

- Android Studio or command-line Android SDK
- JDK 8 or higher
- Gradle 8.0+

### Build Commands

```bash
# Clone the repository
git clone <repository-url>
cd invcolors

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug
```

## Troubleshooting

### Module not working

1. Verify LSPosed framework is installed correctly
2. Check that the module is enabled in LSPosed Manager
3. Ensure you've selected target apps in the module scope
4. Perform a soft reboot after enabling

### App crashes

- Some apps may not be compatible - try disabling the module for that specific app
- Check LSPosed logs for errors

### Colors not inverted

- Ensure the app is in the module scope
- Some UI elements using custom rendering may not be affected
- Check LSPosed logs to verify the module is loading

## Technical Details

### Hook Implementation

The module hooks `android.view.View.draw(Canvas)` and:
1. Saves the canvas state before drawing
2. Applies a ColorMatrixColorFilter with inversion matrix
3. Restores the original canvas state after drawing

### Color Inversion Matrix

```java
float[] matrix = {
    -1,  0,  0,  0, 255,  // Invert red
     0, -1,  0,  0, 255,  // Invert green
     0,  0, -1,  0, 255,  // Invert blue
     0,  0,  0,  1,   0   // Preserve alpha
};
```

## License

This project is provided as-is for educational purposes.

## Disclaimer

This module is intended for accessibility and customization purposes. Use responsibly and at your own risk. The developer is not responsible for any issues arising from the use of this module.
