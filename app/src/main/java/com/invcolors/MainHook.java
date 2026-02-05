package com.invcolors;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "InvColors";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Skip system apps and the module itself
        if (lpparam.packageName.equals("com.invcolors") || 
            lpparam.packageName.equals("android") ||
            lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        XposedBridge.log(TAG + ": Hooking " + lpparam.packageName);
        
        // Apply color inversion filter
        ColorMatrixColorFilter filter = createInversionFilter();
        
        try {
            // Hook ViewGroup.dispatchDraw()
            XposedHelpers.findAndHookMethod(
                ViewGroup.class,
                "dispatchDraw",
                Canvas.class,
                new ColorFilterHook(filter)
            );
            
            // Hook View.draw()
            XposedHelpers.findAndHookMethod(
                View.class,
                "draw",
                Canvas.class,
                new ColorFilterHook(filter)
            );
            
            XposedBridge.log(TAG + ": View hooks applied");
            
            // Hook WebView for Cordova/Capacitor apps
            hookWebView(lpparam);
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error: " + t.getMessage());
        }
    }

    private void hookWebView(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> webViewClass = XposedHelpers.findClass("android.webkit.WebView", lpparam.classLoader);
            
            // Hook loadUrl
            XposedHelpers.findAndHookMethod(webViewClass, "loadUrl", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    injectDarkModeCSS(param.thisObject);
                }
            });
            
            // Hook onPageFinished
            Class<?> webViewClientClass = XposedHelpers.findClass("android.webkit.WebViewClient", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(webViewClientClass, "onPageFinished", 
                android.webkit.WebView.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    injectDarkModeCSS(param.args[0]);
                }
            });
            
            XposedBridge.log(TAG + ": WebView hooks applied");
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": WebView hook failed: " + t.getMessage());
        }
    }
    
    private void injectDarkModeCSS(Object webView) {
        try {
            String script = 
                "(function() {" +
                "  if (document.getElementById('invcolors-dark')) return;" +
                "  var style = document.createElement('style');" +
                "  style.id = 'invcolors-dark';" +
                "  style.innerHTML = '" +
                "    html { filter: invert(1) hue-rotate(180deg) !important; }" +
                "    img, video, [style*=\"background-image\"] { filter: invert(1) hue-rotate(180deg) !important; }" +
                "  ';" +
                "  document.head.appendChild(style);" +
                "})();";
            
            XposedHelpers.callMethod(webView, "evaluateJavascript", script, null);
        } catch (Throwable t) {
            try {
                XposedHelpers.callMethod(webView, "loadUrl", "javascript:" + script);
            } catch (Throwable t2) {
                // Silently fail
            }
        }
    }

    private static class ColorFilterHook extends XC_MethodHook {
        private final ColorMatrixColorFilter filter;
        private Paint mPaint = null;

        ColorFilterHook(ColorMatrixColorFilter filter) {
            this.filter = filter;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                View view = (View) param.thisObject;
                Canvas canvas = (Canvas) param.args[0];
                
                // Only apply to root views
                boolean isRoot = view.getParent() == null || 
                               view.getClass().getName().contains("DecorView");
                
                if (isRoot) {
                    if (mPaint == null) {
                        mPaint = new Paint();
                        mPaint.setColorFilter(filter);
                    }
                    
                    canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), mPaint, 
                        Canvas.ALL_SAVE_FLAG);
                }
            } catch (Throwable t) {
                // Silently ignore
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                View view = (View) param.thisObject;
                boolean isRoot = view.getParent() == null || 
                               view.getClass().getName().contains("DecorView");
                
                if (isRoot) {
                    Canvas canvas = (Canvas) param.args[0];
                    canvas.restore();
                }
            } catch (Throwable t) {
                // Silently ignore
            }
        }
    }

    private static ColorMatrixColorFilter createInversionFilter() {
        ColorMatrix matrix = new ColorMatrix(new float[] {
            -1f,  0f,  0f,  0f, 255f,
             0f, -1f,  0f,  0f, 255f,
             0f,  0f, -1f,  0f, 255f,
             0f,  0f,  0f,  1f,   0f
        });
        return new ColorMatrixColorFilter(matrix);
    }
}
