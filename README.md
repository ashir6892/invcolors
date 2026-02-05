# InvColors - LSPosed Dark Mode Module

A lightweight LSPosed/Xposed module that forces dark mode on Android apps by inverting colors.

## Features

- 🎨 **Native View Inversion** - Inverts colors in native Android views
- 🌐 **WebView Support** - Injects dark mode CSS into WebView-based apps (Cordova, Capacitor, Ionic, etc.)
- ⚡ **Lightweight** - Minimal performance impact
- 🔧 **No UI** - Simple module, just enable in LSPosed and select target apps
- 📱 **Android 7.0+** Compatible (API 24+)

## How It Works

### Native Apps
Hooks `View.draw()` and `ViewGroup.dispatchDraw()` to apply a ColorMatrix that inverts RGB values:
```
R' = 255 - R
G' = 255 - G  
B' = 255 - B
```

### Hybrid/WebView Apps
For apps built with Cordova, Capacitor, or Ionic:
- Hooks `WebView.loadUrl()` and `WebViewClient.onPageFinished()`
- Injects CSS: `filter: invert(1) hue-rotate(180deg)`
- Re-inverts images/videos to display correctly

## Installation

### Requirements
- Rooted Android device
- LSPosed or EdXposed framework
- Android 7.0+ (API 24)

### Steps

1. **Download** the APK from [Releases](https://github.com/ashir6892/invcolors/releases)

2. **Install** the APK on your device

3. **Enable in LSPosed**
   - Open LSPosed Manager → Modules
   - Enable "InvColors"
   - Tap module → Select target apps in Scope

4. **Reboot** (soft reboot is sufficient)

5. **Launch target apps** - dark mode should be active!

## Building from Source

```bash
git clone https://github.com/ashir6892/invcolors.git
cd invcolors
./gradlew assembleRelease
```

APK will be at `app/build/outputs/apk/release/`

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Module not working | Verify LSPosed is installed, module enabled, apps in scope, then reboot |
| App crashes | Disable module for that specific app |
| Colors not inverted | Check LSPosed logs; some custom-rendered UI may not be affected |
| WebView not dark | Force-stop the app and relaunch |

## Technical Details

### Color Inversion Matrix
```java
float[] matrix = {
    -1,  0,  0,  0, 255,  // Invert red
     0, -1,  0,  0, 255,  // Invert green
     0,  0, -1,  0, 255,  // Invert blue
     0,  0,  0,  1,   0   // Preserve alpha
};
```

### WebView CSS Injection
```css
html { filter: invert(1) hue-rotate(180deg) !important; }
img, video { filter: invert(1) hue-rotate(180deg) !important; }
```

## License

This project is provided as-is for educational and accessibility purposes.

## Disclaimer

Use responsibly and at your own risk. The developer is not responsible for any issues arising from use of this module.
