package ink.mol.droidcast_raw

import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.util.Log
import androidx.core.text.isDigitsOnly
import com.koushikdutta.async.http.Multimap
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback
import java.lang.Exception
import java.nio.ByteBuffer

class AnyRequestCallback : HttpServerRequestCallback {
    private var displayUtil: DisplayUtil? = DisplayUtil()

    override fun onRequest(
        request: AsyncHttpServerRequest?,
        response: AsyncHttpServerResponse?
    ) {
        try {
            val pairs: Multimap? = request?.query
            val width: String? = pairs?.getString("width")
            val height: String? = pairs?.getString("height")

            if (!width.isNullOrEmpty() && !height.isNullOrEmpty() && width.isDigitsOnly() && height.isDigitsOnly()) {
                Main.setWH(width.toInt(), height.toInt())
            }

            if (Main.getWidth() == 0 || Main.getHeight() == 0) {
                val point: Point? = displayUtil?.getCurrentDisplaySize()
                if (point != null && point.x > 0 && point.y > 0) {
                    Main.setWH(point.x, point.y)
                } else {
                    Main.setWH(720, 1080)
                }
            }

            val destWidth: Int = Main.getWidth()
            val destHeight: Int = Main.getHeight()

            val bytes: ByteArray = getScreenImageInBytes(destWidth, destHeight)

            response?.send("application/octet-stream", bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            response?.code(500)
            val manufacturer = Build.MANUFACTURER
            val device = Build.DEVICE
            val osVersion = Build.VERSION.RELEASE
            val error =
                ":(  Failed to generate the screenshot on device / emulator : $manufacturer - $device - Android OS : $osVersion"
            response?.send(error)
        }
    }

    private fun getScreenImageInBytes(
        destWidth: Int,
        destHeight: Int
    ): ByteArray {
        var bitmap = ScreenCaptorUtils.screenshot(destWidth, destHeight)
        Log.i("DroidCast_raw_log", "Bitmap generated with resolution $destWidth:$destHeight")

        when (displayUtil?.getScreenRotation()) {
            1 -> bitmap = bitmap.let { displayUtil?.rotateBitmap(it, -90f) }!!
            2 -> bitmap = bitmap.let { displayUtil?.rotateBitmap(it, 90f) }!!
            3 -> bitmap = bitmap.let { displayUtil?.rotateBitmap(it, 180f) }!!
        }

        val buffer = ByteBuffer.allocate((destWidth.times(destHeight)) * 2)
        bitmap.copy(Bitmap.Config.RGB_565, false)?.copyPixelsToBuffer(buffer)
        bitmap.recycle()

        return buffer.array()
    }
}