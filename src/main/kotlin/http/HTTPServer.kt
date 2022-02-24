package http

import Options
import http.payloads.Payload
import io.javalin.Javalin
import util.blue
import util.currentTime

class HTTPServer {
    private val port = Options.httpPort

    fun run() {
        println("[HTTP] Listening on 0.0.0.0:$port".blue())

        val app = Javalin.create().start(port)
        app.get("/*") { ctx ->
            val uri = ctx.req.requestURI
            println("${currentTime()} [HTTP] Receive a request to $uri")
            ctx.result(getPayload(uri.substring(1)))
        }
    }

    private fun getPayload(filename: String): ByteArray =
        Payload(8).generate(filename.substringBefore(".class"))
}
