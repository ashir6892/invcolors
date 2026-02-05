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

public class MainHook implements IXposedHookLoadPackage {

    private static final ColorMatrixColorFilter invertFilter = createInvertFilter();
    private static final String TAG = "InvColors";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Skip system apps and the module itself to avoid issues
        if (lpparam.packageName.equals("com.invcolors") || 
            lpparam.packageName.equals("android") ||
            lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        XposedBridge.log(TAG + ": ========================================");
        XposedBridge.log(TAG + ": Loading for package: " + lpparam.packageName);
        XposedBridge.log(TAG + ": ========================================");

        try {
            // Hook ViewGroup.dispatchDraw() for root views
            XposedHelpers.findAndHookMethod(
                ViewGroup.class,
                "dispatchDraw",
                Canvas.class,
                new XC_MethodHook() {
                    private Paint mPaint = null;
                    private int hookCount = 0;

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ViewGroup viewGroup = (ViewGroup) param.thisObject;
                            Canvas canvas = (Canvas) param.args[0];
                            
                            // Only log first few times to avoid spam
                            if (hookCount < 5) {
                                XposedBridge.log(TAG + ": dispatchDraw called on " + viewGroup.getClass().getSimpleName());
                                hookCount++;
                            }
                            
                            // Check if this is a root view
                            boolean isRoot = viewGroup.getParent() == null || 
                                           viewGroup.getClass().getName().contains("DecorView");
                            
                            if (isRoot) {
                                if (mPaint == null) {
                                    mPaint = new Paint();
                                    mPaint.setColorFilter(invertFilter);
                                    XposedBridge.log(TAG + ": Created inversion paint for root view");
                                }
                                
                                XposedBridge.log(TAG + ": Applying color inversion to root view: " + 
                                               viewGroup.getClass().getSimpleName());
                                
                                // Apply color inversion to entire window canvas
                                canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), mPaint, 
                                    Canvas.ALL_SAVE_FLAG);
                                
                                XposedBridge.log(TAG + ": Successfully applied saveLayer with inversion filter");
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": ERROR in beforeHookedMethod: " + t.getMessage());
                            XposedBridge.log(t);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ViewGroup viewGroup = (ViewGroup) param.thisObject;
                            boolean isRoot = viewGroup.getParent() == null || 
                                           viewGroup.getClass().getName().contains("DecorView");
                            
                            if (isRoot) {
                                Canvas canvas = (Canvas) param.args[0];
                                canvas.restore();
                                XposedBridge.log(TAG + ": Restored canvas after inversion");
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": ERROR in afterHookedMethod: " + t.getMessage());
                        }
                    }
                }
            );

            XposedBridge.log(TAG + ": Successfully hooked ViewGroup.dispatchDraw()");

            // Hook View.draw() as fallback
            XposedHelpers.findAndHookMethod(
                View.class,
                "draw",
                Canvas.class,
                new XC_MethodHook() {
                    private static int drawCount = 0;
                    
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (drawCount < 3) {
                            View view = (View) param.thisObject;
                            XposedBridge.log(TAG + ": View.draw() called on " + view.getClass().getSimpleName());
                            drawCount++;
                        }
                    }
                }
            );

            XposedBridge.log(TAG + ": Successfully hooked View.draw() for monitoring");
            XposedBridge.log(TAG + ": All hooks installed successfully!");
            XposedBridge.log(TAG + ": ========================================");

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": CRITICAL ERROR hooking package " + lpparam.packageName);
            XposedBridge.log(TAG + ": Error message: " + t.getMessage());
            XposedBridge.log(t);
        }
    }

    /**
     * Creates a color inversion filter using ColorMatrix
     * This inverts all RGB values while preserving alpha
     * Same matrix used by Android's accessibility color inversion
     */
    private static ColorMatrixColorFilter createInvertFilter() {
        XposedBridge.log(TAG + ": Creating color inversion filter");
        ColorMatrix colorMatrix = new ColorMatrix(new float[] {
            -1f,  0f,  0f,  0f, 255f,  // Red channel inverted
             0f, -1f,  0f,  0f, 255f,  // Green channel inverted
             0f,  0f, -1f,  0f, 255f,  // Blue channel inverted
             0f,  0f,  0f,  1f,   0f   // Alpha channel unchanged
        });
        XposedBridge.log(TAG + ": Color matrix created successfully");
        return new ColorMatrixColorFilter(colorMatrix);
    }
}
