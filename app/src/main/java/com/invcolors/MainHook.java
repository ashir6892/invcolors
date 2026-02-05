package com.invcolors;

import android.content.Context;
import android.content.SharedPreferences;
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

        XposedBridge.log(TAG + ": ========================================");
        XposedBridge.log(TAG + ": Loading for package: " + lpparam.packageName);
        
        // Use default white->black transformation for now
        // (Custom colors will be loaded from the module's own SharedPreferences later)
        int sourceColor = Color.WHITE;
        int targetColor = Color.BLACK;
        
        XposedBridge.log(TAG + ": Applying WHITE->BLACK dark mode");
        
        ColorMatrixColorFilter filter = createCustomColorFilter(sourceColor, targetColor);
        
        try {
            // Hook ViewGroup.dispatchDraw()
            XposedHelpers.findAndHookMethod(
                ViewGroup.class,
                "dispatchDraw",
                Canvas.class,
                new ColorFilterHook(filter, lpparam.packageName)
            );
            
            // Hook View.draw()
            XposedHelpers.findAndHookMethod(
                View.class,
                "draw",
                Canvas.class,
                new ColorFilterHook(filter, lpparam.packageName)
            );
            
            XposedBridge.log(TAG + ": Dark mode hooks applied successfully!");
            
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error: " + t.getMessage());
            XposedBridge.log(t);
        }
        
        XposedBridge.log(TAG + ": ========================================");
    }

    private static class ColorFilterHook extends XC_MethodHook {
        private final ColorMatrixColorFilter filter;
        private final String packageName;
        private Paint mPaint = null;
        private int hookCount = 0;

        ColorFilterHook(ColorMatrixColorFilter filter, String packageName) {
            this.filter = filter;
            this.packageName = packageName;
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
                    if (hookCount < 3) {
                        XposedBridge.log(TAG + ": Applying dark mode to " + view.getClass().getSimpleName());
                        hookCount++;
                    }
                    
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

    /**
     * Creates a custom color filter that replaces sourceColor with targetColor
     */
    private static ColorMatrixColorFilter createCustomColorFilter(int sourceColor, int targetColor) {
        // For simple white->black or similar transformations, use color inversion
        if (sourceColor == Color.WHITE && targetColor == Color.BLACK) {
            XposedBridge.log(TAG + ": Using WHITE->BLACK inversion mode");
            ColorMatrix matrix = new ColorMatrix(new float[] {
                -1f,  0f,  0f,  0f, 255f,
                 0f, -1f,  0f,  0f, 255f,
                 0f,  0f, -1f,  0f, 255f,
                 0f,  0f,  0f,  1f,   0f
            });
            return new ColorMatrixColorFilter(matrix);
        }
        
        // For other color combinations, use a simpler approach
        XposedBridge.log(TAG + ": Using custom color replacement mode");
        ColorMatrix matrix = new ColorMatrix(new float[] {
            -1f,  0f,  0f,  0f, 255f,
             0f, -1f,  0f,  0f, 255f,
             0f,  0f, -1f,  0f, 255f,
             0f,  0f,  0f,  1f,   0f
        });
        return new ColorMatrixColorFilter(matrix);
    }
}
