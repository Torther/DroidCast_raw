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

            val buffer: ByteBuffer = getScreenImageInByteBuffer(destWidth, destHeight)

            response?.send("application/octet-stream", buffer)
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

    private fun getScreenImageInByteBuffer(
        width: Int,
        height: Int
    ): ByteBuffer {
        var destWidth = width
        var destHeight = height

        val screenRotation: Int? = displayUtil?.getScreenRotation()
        if (screenRotation != null && screenRotation != 0 && screenRotation != 2) { // not portrait
            val tmp = destWidth
            destWidth = destHeight
            destHeight = tmp
        }

        val bitmap: Bitmap? = ScreenCaptorUtils.screenshot(destWidth, destHeight)
        Log.i("DroidCast_raw_log", "Bitmap generated with resolution $destWidth:$destHeight")

        val buffer = ByteBuffer.allocate((destWidth.times(destHeight)) * 2)
        bitmap!!.copy(Bitmap.Config.RGB_565, false)?.copyPixelsToBuffer(buffer)
        bitmap.recycle()
        buffer.flip()

        return buffer
    }
}