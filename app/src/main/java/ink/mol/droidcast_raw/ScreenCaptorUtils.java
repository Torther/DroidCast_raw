package ink.mol.droidcast_raw;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
public final class ScreenCaptorUtils {

    private static final String METHOD_SCREENSHOT = "screenshot";
    private static final Class<?> surfaceControlClass;
    private static Method getBuiltInDisplayMethod;

    static {
        try {
            surfaceControlClass = Class.forName("android.view.SurfaceControl");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Bitmap screenshot(int width, int height) {
        Bitmap bitmap = null;

        try {
            Method declaredMethod;

            int sdkInt = Build.VERSION.SDK_INT;
            if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { //Upside Down Cake+
                //Need to find new method to get screenshot
                return null;
            } else if (sdkInt >= Build.VERSION_CODES.S) { //Snow Cone+
                // Create the DisplayCaptureArgs object using DisplayCaptureArgs$Builder.build()
                Class<?> displayCaptureArgsClass = Class.forName("android.view.SurfaceControl$DisplayCaptureArgs");
                Class<?> displayCaptureArgsBuilderClass = Class.forName("android.view.SurfaceControl$DisplayCaptureArgs$Builder");
                @SuppressLint("BlockedPrivateApi") Method setSizeMethod = displayCaptureArgsBuilderClass.getDeclaredMethod("setSize", int.class, int.class);
                @SuppressLint("BlockedPrivateApi") Method buildMethod = displayCaptureArgsBuilderClass.getDeclaredMethod("build");

                Constructor<?> constructor = displayCaptureArgsBuilderClass.getDeclaredConstructor(IBinder.class);
                Object builder = constructor.newInstance(getBuiltInDisplay());
                setSizeMethod.invoke(builder, width, height);
                Object args = buildMethod.invoke(builder);

                // Call the hidden method "ScreenshotHardwareBuffer captureDisplay(DisplayCaptureArgs captureArgs)"
                Method captureDisplay = surfaceControlClass.getDeclaredMethod("captureDisplay", displayCaptureArgsClass);
                Object hardwareBuffer = captureDisplay.invoke(null, args);

                if (hardwareBuffer != null) {
                    Class<?> hardwareBufferClass = Class.forName("android.view.SurfaceControl$ScreenshotHardwareBuffer");
                    @SuppressLint("BlockedPrivateApi") Method asBitmap = hardwareBufferClass.getDeclaredMethod("asBitmap");
                    bitmap = (Bitmap) asBitmap.invoke(hardwareBuffer);
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
            Method method = getGetBuiltInDisplayMethod();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Default display 0
                return (IBinder) method.invoke(null, 0);
            }
            return (IBinder) method.invoke(null);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }
}
