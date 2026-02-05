package com.invcolors;

import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final ColorMatrixColorFilter invertFilter = createInvertFilter();
    private static Paint filterPaint = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Skip system apps and the module itself to avoid issues
        if (lpparam.packageName.equals("com.invcolors") || 
            lpparam.packageName.equals("android") ||
            lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        XposedBridge.log("InvColors: Hooking package: " + lpparam.packageName);

        try {
            // Hook View.onDraw() to apply color inversion
            XposedHelpers.findAndHookMethod(
                View.class,
                "onDraw",
                Canvas.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Canvas canvas = (Canvas) param.args[0];
                        
                        if (filterPaint == null) {
                            filterPaint = new Paint();
                            filterPaint.setColorFilter(invertFilter);
                        }
                        
                        // Save canvas and apply color filter
                        canvas.save();
                        canvas.saveLayer(null, filterPaint, Canvas.ALL_SAVE_FLAG);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Canvas canvas = (Canvas) param.args[0];
                        
                        // Restore canvas state
                        canvas.restore();
                        canvas.restore();
                    }
                }
            );

            // Also hook dispatchDraw for ViewGroups to ensure child views are inverted
            XposedHelpers.findAndHookMethod(
                View.class,
                "dispatchDraw",
                Canvas.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Canvas canvas = (Canvas) param.args[0];
                        
                        if (filterPaint == null) {
                            filterPaint = new Paint();
                            filterPaint.setColorFilter(invertFilter);
                        }
                        
                        canvas.save();
                        canvas.saveLayer(null, filterPaint, Canvas.ALL_SAVE_FLAG);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Canvas canvas = (Canvas) param.args[0];
                        
                        canvas.restore();
                        canvas.restore();
                    }
                }
            );

        } catch (Throwable t) {
            XposedBridge.log("InvColors: Error hooking package " + lpparam.packageName);
            XposedBridge.log(t);
        }
    }

    /**
     * Creates a color inversion filter using ColorMatrix
     * This inverts all RGB values while preserving alpha
     */
    private static ColorMatrixColorFilter createInvertFilter() {
        ColorMatrix colorMatrix = new ColorMatrix(new float[] {
            -1f,  0f,  0f,  0f, 255f,  // Red channel inverted
             0f, -1f,  0f,  0f, 255f,  // Green channel inverted
             0f,  0f, -1f,  0f, 255f,  // Blue channel inverted
             0f,  0f,  0f,  1f,   0f   // Alpha channel unchanged
        });
        return new ColorMatrixColorFilter(colorMatrix);
    }
}
