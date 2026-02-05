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
            // Hook View.draw() to apply color inversion
            XposedHelpers.findAndHookMethod(
                    View.class,
                    "draw",
                    Canvas.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Canvas canvas = (Canvas) param.args[0];

                            // Save canvas state
                            canvas.save();

                            // Apply color inversion filter
                            Paint paint = new Paint();
                            paint.setColorFilter(invertFilter);
                            canvas.saveLayer(null, paint);
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Canvas canvas = (Canvas) param.args[0];

                            // Restore canvas state (removes the color filter)
                            canvas.restore();
                            canvas.restore();
                        }
                    });

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
                -1f, 0f, 0f, 0f, 255f, // Red channel inverted
                0f, -1f, 0f, 0f, 255f, // Green channel inverted
                0f, 0f, -1f, 0f, 255f, // Blue channel inverted
                0f, 0f, 0f, 1f, 0f // Alpha channel unchanged
        });
        return new ColorMatrixColorFilter(colorMatrix);
    }
}
