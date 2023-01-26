package com.torther.droidcasts;

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
import java.util.Objects;

/**
 * Created by seanzhou on 3/14/17.
 * Modify by Torther on 17/1/23
 */
@SuppressLint("PrivateApi")
public final class ScreenCaptorUtils {

    private static final String METHOD_SCREENSHOT = "screenshot";

    private static final String CLASS_NAME = "android.view.SurfaceControl";

    private static final Class<?> anyClass;
    private static Method getBuiltInDisplayMethod;

    static {
        try {
            anyClass = Class.forName(CLASS_NAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param imageWidth  image width
     * @param imageHeight image height
     * @return Bitmap(Config = HARDWARE)
     */
    public static Bitmap screenshot(int imageWidth, int imageHeight) {
        Bitmap bitmap = null;

        try {
            Method declaredMethod;
            int sdkInt = Build.VERSION.SDK_INT;

            if (sdkInt < Build.VERSION_CODES.P /* Android 9 */) {
                declaredMethod = anyClass.getDeclaredMethod(METHOD_SCREENSHOT, Integer.TYPE, Integer.TYPE);
                bitmap = (Bitmap) declaredMethod.invoke(null, new Object[]{imageWidth, imageHeight});
            } else if (sdkInt < Build.VERSION_CODES.S /* Android 12 */) {
                declaredMethod = anyClass.getDeclaredMethod(METHOD_SCREENSHOT, Rect.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                bitmap = (Bitmap) declaredMethod.invoke(null, new Rect(), imageWidth, imageHeight, 0);
            }else {
                // Create the DisplayCaptureArgs object by DisplayCaptureArgs$Builder.build()
                Class<?> argsClass = Class.forName("android.view.SurfaceControl$DisplayCaptureArgs");
                Class<?> innerClass = Class.forName("android.view.SurfaceControl$DisplayCaptureArgs$Builder");

                @SuppressLint("BlockedPrivateApi") Method setSzMethod = innerClass.getDeclaredMethod("setSize", int.class, int.class);
                @SuppressLint("BlockedPrivateApi") Method buildMethod = innerClass.getDeclaredMethod("build");

                Constructor<?> anyConstructor = innerClass.getDeclaredConstructor(IBinder.class);
                Object builder = anyConstructor.newInstance(getBuiltInDisplay());
                setSzMethod.invoke(builder, imageWidth, imageHeight);
                Object args = buildMethod.invoke(builder);

                // call hidden method "ScreenshotHardwareBuffer captureDisplay(DisplayCaptureArgs captureArgs)"
                Method captureDisplay = anyClass.getDeclaredMethod("captureDisplay", argsClass);
                Object hdBuffer = captureDisplay.invoke(null, args);

                Class<?> hdBufferClass = Objects.requireNonNull(hdBuffer).getClass();
                ColorSpace colorSpace = (ColorSpace) hdBufferClass.getDeclaredMethod("getColorSpace").invoke(hdBuffer);
                System.out.println(">>> ColorSpace: " + colorSpace);
                HardwareBuffer hardwareBuffer = (HardwareBuffer) hdBufferClass.getDeclaredMethod("getHardwareBuffer").invoke(hdBuffer);

                bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace);
            }

            if (bitmap != null) System.out.println(">>> bmp generated.");

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 ClassNotFoundException | InstantiationException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    /**
     * @return Method android.view.SurfaceControl.getBuiltInDisplay         SDK < Android 10
     * and Method android.view.SurfaceControl.getInternalDisplayToken   SDK >= Android 10
     */
    private static Method getGetBuiltInDisplayMethod() throws NoSuchMethodException {
        if (getBuiltInDisplayMethod == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q /* Android 10 */) {
                getBuiltInDisplayMethod = anyClass.getMethod("getBuiltInDisplay", Integer.TYPE);
            } else {
                // The method signature has been changed in Android Q+
                getBuiltInDisplayMethod = anyClass.getMethod("getInternalDisplayToken");
            }
        }
        return getBuiltInDisplayMethod;
    }

    /**
     * @return IBinder android.view.SurfaceControl.getInternalDisplayToken(null, 0)
     */
    public static IBinder getBuiltInDisplay() {

        try {
            Method method = getGetBuiltInDisplayMethod();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q /* Android 10 */) {
                // default display 0
                return (IBinder) method.invoke(null, 0);
            }
            return (IBinder) method.invoke(null);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            System.err.println("Failed to invoke method " + e);
            return null;
        }
    }
}
