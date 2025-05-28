package ink.mol.droidcast_raw

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.IBinder
import android.util.Log
import ink.mol.droidcast_raw.warpper.DisplayControl
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.nio.ByteBuffer

@SuppressLint("PrivateApi", "UnsafeDynamicallyLoadedCode")
object ScreenCaptorUtils {
    private var surfaceControlClass: Class<*>? = null
    private var getBuiltInDisplayMethod: Method? = null
    private val sdkInt: Int = Build.VERSION.SDK_INT
    private var nativeCopy = sdkInt >= Build.VERSION_CODES.S && NativeLibHelper.loadLibs()

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

    data class CachedArgs(val width: Int, val height: Int, val pixfmt: Int?, val args: Any)
    var cachedArgs: CachedArgs? = null

    @SuppressLint("NewApi", "BlockedPrivateApi")
    fun screenshot(width: Int, height: Int, pixfmt: Int? = null): Bitmap? {
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

                val localCachedArgs = cachedArgs
                val args = if (localCachedArgs?.height == height && localCachedArgs.width == width && localCachedArgs.pixfmt == pixfmt) {
                    localCachedArgs.args
                } else {
                    val setSizeMethod: Method =
                        displayCaptureArgsBuilderClass.getDeclaredMethod(
                            "setSize",
                            Int::class.java,
                            Int::class.java
                        )
                    val setPixelFormatMethod: Method =
                        displayCaptureArgsBuilderClass.getDeclaredMethod(
                            "setPixelFormat",
                            Int::class.java
                        )
                    val buildMethod: Method =
                        displayCaptureArgsBuilderClass.getDeclaredMethod("build")

                    val constructor: Constructor<*> =
                        displayCaptureArgsBuilderClass.getDeclaredConstructor(IBinder::class.java)
                    val builder: Any = constructor.newInstance(getBuiltInDisplay())
                    setSizeMethod.invoke(builder, width, height)
                    pixfmt?.let {
                        setPixelFormatMethod.invoke(builder, it)
                    }
                    val args = buildMethod.invoke(builder)!!
                    cachedArgs = CachedArgs(width, height, pixfmt, args)
                    args
                }

                val captureDisplay: Method = surfaceControlClass!!.getDeclaredMethod(
                    "captureDisplay",
                    displayCaptureArgsClass
                )
                val hardwareBuffer = captureDisplay.invoke(null, args)!!

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

    @SuppressLint("NewApi")
    fun copyBitmapToBuffer(bitmap: Bitmap, byteBuffer: ByteBuffer) {
        if (nativeCopy && bitmap.config == Bitmap.Config.HARDWARE && byteBuffer.isDirect) {
            val hardwareBuffer = bitmap.hardwareBuffer
            val pos = byteBuffer.position()
            val copied = copyHardwareBufferToByteBufferNative(hardwareBuffer, byteBuffer, pos, byteBuffer.limit())
            if (copied >= 0) {
                byteBuffer.position(pos + copied)
                return
            }
            // Native copy not supported
            nativeCopy = false
        }
        bitmap.copy(Bitmap.Config.RGB_565, false)!!.copyPixelsToBuffer(byteBuffer)
    }

    @SuppressLint("NewApi")
    fun copyBitmapToBufferLz4(bitmap: Bitmap, byteBuffer: ByteBuffer) {
        if (nativeCopy && bitmap.config == Bitmap.Config.HARDWARE && byteBuffer.isDirect) {
            val hardwareBuffer = bitmap.hardwareBuffer
            val pos = byteBuffer.position()
            val copied = copyHardwareBufferToByteBufferNativeLz4(hardwareBuffer, byteBuffer, pos, byteBuffer.limit())
            if (copied >= 0) {
                byteBuffer.position(pos + copied)
                return
            }
            // Native copy not supported
            nativeCopy = false
        }

        val pos = byteBuffer.position()
        val copied = copyBitmapToByteBufferNativeLz4(
            bitmap.copy(Bitmap.Config.RGB_565, false)!!, byteBuffer, pos, byteBuffer.limit())
        if (copied >= 0) {
            byteBuffer.position(pos + copied)
        }
    }

    private external fun copyHardwareBufferToByteBufferNative(hardwareBuffer: HardwareBuffer, byteBuffer: ByteBuffer, position: Int, limit: Int): Int
    private external fun copyHardwareBufferToByteBufferNativeLz4(hardwareBuffer: HardwareBuffer, byteBuffer: ByteBuffer, position: Int, limit: Int): Int
    private external fun copyBitmapToByteBufferNativeLz4(bitmap: Bitmap, byteBuffer: ByteBuffer, position: Int, limit: Int): Int
    external fun lz4CompressBound(dataSize: Int): Int
}