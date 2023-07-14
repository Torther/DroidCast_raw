package ink.mol.droidcast_raw

import android.os.Handler
import android.os.Looper
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse


class KtMain {

    private val sTAG: String = Main::class.java.name
    private var port = 53516
    private var handler: Handler? = null

    fun main(args: Array<String>) {
        resolveArgs(args)
        val httpServer = object : AsyncHttpServer() {
            override fun onRequest(
                request: AsyncHttpServerRequest?,
                response: AsyncHttpServerResponse?
            ): Boolean {
                return super.onRequest(request, response)
            }
        }

        Looper.prepare()
        val looper: Looper? = Looper.myLooper()
        println(">>> DroidCast Started")

        handler = Handler(looper!!)

        val server = AsyncServer()
        httpServer.get("/screenshot", AnyRequestCallback())
        httpServer.get("/preview",AnyRequestCallbackPreview())
        httpServer.listen(server, port)
        Looper.loop()
    }

    private fun resolveArgs(args: Array<String>) {
        if (args.isNotEmpty()) {
            val params: List<String> = args[0].split("=")
            if (params.size == 2 && "--port" == params[0]) {
                port = params[1].toIntOrNull()!!
                println("$sTAG | Port set to $port")
            }
        }
    }
}