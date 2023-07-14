package ink.mol.droidcast_raw

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.os.IBinder
import android.util.Log
import android.view.IWindowManager
import java.lang.Exception
import java.lang.reflect.Method

@SuppressLint("PrivateApi", "DiscouragedPrivateApi")
class DisplayUtil {

    private var iWindowManager: IWindowManager? = null

    init {
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getService: Method =
                serviceManagerClass.getDeclaredMethod("getService", String::class.java)
            val ws: Any? = getService.invoke(null, Context.WINDOW_SERVICE)
            iWindowManager = IWindowManager.Stub.asInterface(ws as IBinder?)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentDisplaySize(): Point {
        return try {
            val localPoint = Point()
            iWindowManager?.getInitialDisplaySize(0, localPoint)
//            println(">>> Dimension: $localPoint")
            localPoint
        } catch (e: Exception) {
            e.printStackTrace()
            Point()
        }
    }

    fun getScreenRotation(): Int {
        var rotation = 0
        try {
            val cls: Class<*> = iWindowManager!!.javaClass
            rotation = try {
                cls.getMethod("getRotation").invoke(iWindowManager) as Int
            } catch (e: NoSuchMethodException) {
                cls.getMethod("getDefaultDisplayRotation").invoke(iWindowManager) as Int
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
//        println(">>> Screen rotation: $rotation")
        Log.i("DroidCast_raw_log", ">>> Screen rotation: $rotation")
        return rotation
    }

    fun rotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }
}