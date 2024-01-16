package ink.mol.droidcast_raw

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.IBinder
import ink.mol.droidcast_raw.warpper.DisplayControl
import java.lang.reflect.Constructor
import java.lang.reflect.Method

@SuppressLint("PrivateApi")
class ScreenCaptorUtils {
    companion object {
        private var surfaceControlClass: Class<*>? = null
        private var getBuiltInDisplayMethod: Method? = null
        private val sdkInt: Int = Build.VERSION.SDK_INT

        init {
            try {
                surfaceControlClass = if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    Class.forName("android.window.ScreenCapture")
                } else {
                    Class.forName("android.view.SurfaceControl")
                }
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(e)
            }
        }

        private fun getBuiltInDisplayMethod(): Method {
            if (getBuiltInDisplayMethod == null) {
                getBuiltInDisplayMethod = if (sdkInt < Build.VERSION_CODES.Q) {
                    surfaceControlClass?.getMethod(
                        "getBuiltInDisplay",
                        Int::class.javaPrimitiveType
                    )
                } else {
                    surfaceControlClass?.getMethod("getInternalDisplayToken")
                }
            }
            return getBuiltInDisplayMethod!!
        }

        private fun getBuiltInDisplay(): IBinder? {
            try {
                if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val displayIds: LongArray? = DisplayControl.getPhysicalDisplayIds()
                    if (displayIds != null) {
                        for (id: Long in displayIds) {
                            return DisplayControl.getPhysicalDisplayToken(id)
                        }
                        return DisplayControl.getPhysicalDisplayToken(0)
                    }
                }
                val method: Method = getBuiltInDisplayMethod()
                return if (sdkInt < Build.VERSION_CODES.Q) {
                    method.invoke(null, 0) as IBinder
                } else {
                    method.invoke(null) as IBinder
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }


        @SuppressLint("NewApi")
        fun screenshot(width: Int, height: Int): Bitmap? {
            var bitmap: Bitmap? = null

            try {
                val declaredMethod: Method

                if (sdkInt >= Build.VERSION_CODES.S) {
                    val displayCaptureArgsClass: Class<*>
                    val displayCaptureArgsBuilderClass: Class<*>

                    if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        displayCaptureArgsClass =
                            Class.forName("android.window.ScreenCapture\$DisplayCaptureArgs")
                        displayCaptureArgsBuilderClass =
                            Class.forName("android.window.ScreenCapture\$DisplayCaptureArgs\$Builder")
                    } else {
                        displayCaptureArgsClass =
                            Class.forName("android.view.SurfaceControl\$DisplayCaptureArgs")
                        displayCaptureArgsBuilderClass =
                            Class.forName("android.view.SurfaceControl\$DisplayCaptureArgs\$Builder")
                    }

                    @SuppressLint("BlockedPrivateApi") val setSizeMethod: Method =
                        displayCaptureArgsBuilderClass.getDeclaredMethod(
                            "setSize",
                            Int::class.java,
                            Int::class.java
                        )
                    @SuppressLint("BlockedPrivateApi") val buildMethod: Method =
                        displayCaptureArgsBuilderClass.getDeclaredMethod("build")

                    val constructor: Constructor<*> =
                        displayCaptureArgsBuilderClass.getDeclaredConstructor(IBinder::class.java)
                    val builder: Any = constructor.newInstance(getBuiltInDisplay())
                    setSizeMethod.invoke(builder, width, height)
                    val args: Any? = buildMethod.invoke(builder)

                    val captureDisplay: Method = surfaceControlClass!!.getDeclaredMethod(
                        "captureDisplay",
                        displayCaptureArgsClass
                    )
                    val hardwareBuffer = captureDisplay.invoke(null, args)

                    val hardwareBufferClass: Class<*> = hardwareBuffer.javaClass
                    val colorSpace: ColorSpace =
                        hardwareBufferClass.getDeclaredMethod("getColorSpace")
                            .invoke(hardwareBuffer) as ColorSpace

                    (hardwareBufferClass.getDeclaredMethod("getHardwareBuffer")
                        .invoke(hardwareBuffer) as HardwareBuffer).use { buffer ->
                        bitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace)
                    }
                } else if (sdkInt >= Build.VERSION_CODES.P) {
                    declaredMethod = surfaceControlClass!!.getDeclaredMethod(
                        "screenshot",
                        Rect::class.java,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                    bitmap = declaredMethod.invoke(null, Rect(), width, height, 0) as Bitmap
                } else {
                    declaredMethod = surfaceControlClass!!.getDeclaredMethod(
                        "screenshot",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                    bitmap = declaredMethod.invoke(null, width, height) as Bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return bitmap

        }
    }
}