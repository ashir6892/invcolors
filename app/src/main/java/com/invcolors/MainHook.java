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

    private static final ColorMatrixColorFilter darkModeFilter = createDarkModeFilter();
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
        XposedBridge.log(TAG + ": Loading DARK MODE for package: " + lpparam.packageName);
        XposedBridge.log(TAG + ": ========================================");

        try {
            // Hook ViewGroup.dispatchDraw() for comprehensive dark mode application
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
                            
                            // Log first few times
                            if (hookCount < 3) {
                                XposedBridge.log(TAG + ": dispatchDraw on " + viewGroup.getClass().getSimpleName());
                                hookCount++;
                            }
                            
                            // Check if this is a root view (DecorView or no parent)
                            boolean isRoot = viewGroup.getParent() == null || 
                                           viewGroup.getClass().getName().contains("DecorView");
                            
                            if (isRoot) {
                                if (mPaint == null) {
                                    mPaint = new Paint();
                                    mPaint.setColorFilter(darkModeFilter);
                                    XposedBridge.log(TAG + ": Created DARK MODE paint for root view");
                                }
                                
                                XposedBridge.log(TAG + ": Applying DARK MODE to: " + viewGroup.getClass().getSimpleName());
                                
                                // Apply dark mode filter to entire canvas
                                canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), mPaint, 
                                    Canvas.ALL_SAVE_FLAG);
                                
                                XposedBridge.log(TAG + ": DARK MODE applied successfully");
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": ERROR in beforeHook: " + t.getMessage());
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
                            }
                        } catch (Throwable t) {
                            // Silently ignore
                        }
                    }
                }
            );

            XposedBridge.log(TAG + ": Successfully hooked ViewGroup.dispatchDraw() for DARK MODE");

            // Also hook View.draw() for better coverage
            XposedHelpers.findAndHookMethod(
                View.class,
                "draw",
                Canvas.class,
                new XC_MethodHook() {
                    private Paint mPaint = null;
                    
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            View view = (View) param.thisObject;
                            
                            // Only apply to root-level views
                            if (view.getParent() == null) {
                                Canvas canvas = (Canvas) param.args[0];
                                
                                if (mPaint == null) {
                                    mPaint = new Paint();
                                    mPaint.setColorFilter(darkModeFilter);
                                }
                                
                                canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), mPaint, 
                                    Canvas.ALL_SAVE_FLAG);
                                
                                XposedBridge.log(TAG + ": Applied DARK MODE via View.draw()");
                            }
                        } catch (Throwable t) {
                            // Silently ignore
                        }
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            View view = (View) param.thisObject;
                            if (view.getParent() == null) {
                                Canvas canvas = (Canvas) param.args[0];
                                canvas.restore();
                            }
                        } catch (Throwable t) {
                            // Silently ignore
                        }
                    }
                }
            );

            XposedBridge.log(TAG + ": All DARK MODE hooks installed successfully!");
            XposedBridge.log(TAG + ": ========================================");

        } catch (Throwable t) {
            XposedBridge.log(TAG + ": CRITICAL ERROR: " + t.getMessage());
            XposedBridge.log(t);
        }
    }

    /**
     * Creates a dark mode filter for pure AMOLED black backgrounds
     * This creates a sophisticated dark theme:
     * - Converts light backgrounds to pure black (#000000)
     * - Converts dark text to light/white
     * - Maintains reasonable contrast
     */
    private static ColorMatrixColorFilter createDarkModeFilter() {
        XposedBridge.log(TAG + ": Creating PURE DARK MODE filter");
        
        // This matrix does:
        // 1. Inverts colors (light becomes dark, dark becomes light)
        // 2. Reduces overall brightness for deeper blacks
        // 3. Increases contrast for better readability
        
        ColorMatrix invertMatrix = new ColorMatrix(new float[] {
            -1f,  0f,  0f,  0f, 255f,  // Invert red
             0f, -1f,  0f,  0f, 255f,  // Invert green
             0f,  0f, -1f,  0f, 255f,  // Invert blue
             0f,  0f,  0f,  1f,   0f   // Keep alpha
        });
        
        // Apply contrast and brightness adjustments for deeper blacks
        ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
            1.2f, 0f,   0f,   0f, -25f,  // Increase contrast, reduce brightness
            0f,   1.2f, 0f,   0f, -25f,
            0f,   0f,   1.2f, 0f, -25f,
            0f,   0f,   0f,   1f,   0f
        });
        
        // Combine matrices
        invertMatrix.postConcat(contrastMatrix);
        
        XposedBridge.log(TAG + ": DARK MODE filter created - AMOLED pure black mode");
        return new ColorMatrixColorFilter(invertMatrix);
    }
}
