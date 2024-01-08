package ink.mol.droidcast_raw;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ink.mol.droidcast_raw.warpper.DisplayControl;

@SuppressLint("PrivateApi")
public final class ScreenCaptorUtils {

    private static final String METHOD_SCREENSHOT = "screenshot";
    private static final Class<?> surfaceControlClass;
    private static Method getBuiltInDisplayMethod;
    private static final int sdkInt = Build.VERSION.SDK_INT;

    static {
        try {
            if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                surfaceControlClass = Class.forName("android.window.ScreenCapture");
            } else {
                surfaceControlClass = Class.forName("android.view.SurfaceControl");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Bitmap screenshot(int width, int height) {
        Bitmap bitmap = null;

        try {
            Method declaredMethod;

            if (sdkInt >= Build.VERSION_CODES.S) { //Snow Cone+
                // Create the DisplayCaptureArgs object using DisplayCaptureArgs$Builder.build()
                Class<?> displayCaptureArgsClass;
                Class<?> displayCaptureArgsBuilderClass;
                if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    displayCaptureArgsClass = Class.forName("android.window.ScreenCapture$DisplayCaptureArgs");
                    displayCaptureArgsBuilderClass = Class.forName("android.window.ScreenCapture$DisplayCaptureArgs$Builder");
                } else {
                    displayCaptureArgsClass = Class.forName("android.view.SurfaceControl$DisplayCaptureArgs");
                    displayCaptureArgsBuilderClass = Class.forName("android.view.SurfaceControl$DisplayCaptureArgs$Builder");
                }
                @SuppressLint("BlockedPrivateApi") Method setSizeMethod = displayCaptureArgsBuilderClass.getDeclaredMethod("setSize", int.class, int.class);
                @SuppressLint("BlockedPrivateApi") Method buildMethod = displayCaptureArgsBuilderClass.getDeclaredMethod("build");

                Constructor<?> constructor = displayCaptureArgsBuilderClass.getDeclaredConstructor(IBinder.class);
                Object builder = constructor.newInstance(getBuiltInDisplay());
                setSizeMethod.invoke(builder, width, height);
                Object args = buildMethod.invoke(builder);

                // Call the hidden method "ScreenshotHardwareBuffer captureDisplay(DisplayCaptureArgs captureArgs)"
                Method captureDisplay = surfaceControlClass.getDeclaredMethod("captureDisplay", displayCaptureArgsClass);
                Object hardwareBuffer = captureDisplay.invoke(null, args);

                Class<?> hardwareBufferClass = hardwareBuffer.getClass();
                ColorSpace colorSpace = (ColorSpace) hardwareBufferClass.getDeclaredMethod("getColorSpace").invoke(hardwareBuffer);

                try (HardwareBuffer buffer = (HardwareBuffer) hardwareBufferClass.getDeclaredMethod("getHardwareBuffer").invoke(hardwareBuffer)) {
                    bitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (sdkInt >= Build.VERSION_CODES.P) { // Pie+
                declaredMethod = surfaceControlClass.getDeclaredMethod(
                        METHOD_SCREENSHOT,
                        Rect.class,
                        Integer.TYPE,
                        Integer.TYPE,
                        Integer.TYPE);
                bitmap = (Bitmap) declaredMethod.invoke(null, new Rect(), width, height, 0);
            } else {
                declaredMethod = surfaceControlClass.getDeclaredMethod(METHOD_SCREENSHOT, Integer.TYPE, Integer.TYPE);
                bitmap = (Bitmap) declaredMethod.invoke(null, new Object[]{width, height});
            }

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException | InstantiationException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private static Method getGetBuiltInDisplayMethod() throws NoSuchMethodException {
        if (getBuiltInDisplayMethod == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                getBuiltInDisplayMethod = surfaceControlClass.getMethod("getBuiltInDisplay", Integer.TYPE);
            } else { // The method signature has been changed in Android Q+
                getBuiltInDisplayMethod = surfaceControlClass.getMethod("getInternalDisplayToken");
            }
        }
        return getBuiltInDisplayMethod;
    }

    public static IBinder getBuiltInDisplay() {
        try {
            if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                long[] displayIds = DisplayControl.getPhysicalDisplayIds();
                if (displayIds != null) {
                    for (long id : displayIds) {
                        IBinder binder = DisplayControl.getPhysicalDisplayToken(id);
                        if (binder != null) return binder;
                    }
                    return DisplayControl.getPhysicalDisplayToken(0);
                }
            }
            Method method = getGetBuiltInDisplayMethod();
            return (IBinder) (sdkInt < Build.VERSION_CODES.Q ? method.invoke(null, 0) : method.invoke(null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
