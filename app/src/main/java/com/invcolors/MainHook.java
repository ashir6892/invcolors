package com.invcolors;

import android.graphics.Canvas;
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

import java.lang.reflect.Field;

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
            // Hook View's setLayerType to force software rendering with color filter
            // This is similar to how Android's accessibility color inversion works
            XposedHelpers.findAndHookMethod(
                View.class,
                "setLayerPaint",
                Paint.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Paint paint = (Paint) param.args[0];
                        if (paint == null) {
                            paint = new Paint();
                        }
                        paint.setColorFilter(invertFilter);
                        param.args[0] = paint;
                    }
                }
            );

            // Hook the root view to apply inversion at window level
            XposedHelpers.findAndHookMethod(
                ViewGroup.class,
                "dispatchDraw",
                Canvas.class,
                new XC_MethodHook() {
                    private Paint mPaint = null;

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ViewGroup viewGroup = (ViewGroup) param.thisObject;
                            
                            // Only apply to root views (DecorView)
                            if (viewGroup.getParent() == null || viewGroup.getParent().toString().contains("ViewRootImpl")) {
                                Canvas canvas = (Canvas) param.args[0];
                                
                                if (mPaint == null) {
                                    mPaint = new Paint();
                                    mPaint.setColorFilter(invertFilter);
                                }
                                
                                // Apply color inversion to entire window canvas
                                canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), mPaint, 
                                    Canvas.ALL_SAVE_FLAG);
                            }
                        } catch (Throwable t) {
                            // Silently ignore errors to avoid spam
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ViewGroup viewGroup = (ViewGroup) param.thisObject;
                            
                            if (viewGroup.getParent() == null || viewGroup.getParent().toString().contains("ViewRootImpl")) {
                                Canvas canvas = (Canvas) param.args[0];
                                canvas.restore();
                            }
                        } catch (Throwable t) {
                            // Silently ignore errors
                        }
                    }
                }
            );

            // Alternative approach: Hook Paint directly for all drawing operations
            XposedHelpers.findAndHookMethod(
                Paint.class,
                "setColorFilter",
                android.graphics.ColorFilter.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // If no color filter is set, apply our inversion filter
                        Paint paint = (Paint) param.thisObject;
                        if (param.args[0] == null) {
                            XposedHelpers.callMethod(param.thisObject, "setColorFilter", invertFilter);
                        }
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
     * Same matrix used by Android's accessibility color inversion
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
