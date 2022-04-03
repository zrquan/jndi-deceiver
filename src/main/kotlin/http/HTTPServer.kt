package http

import util.Options
import io.javalin.Javalin
import util.blue
import util.currentTime

class HTTPServer {
    private val port = Options.httpPort

    fun run() {
        val app = Javalin.create().start(port)
        println("HTTP server listening on 0.0.0.0:$port".blue())

        app.get("/*") { ctx ->
            val uri = ctx.req.requestURI
            log("Receive a request to $uri")
            ctx.result(getPayload(uri.substring(1)))
        }
    }

    private fun getPayload(filename: String): ByteArray =
        PayloadGenerator().generate(filename.substringBefore(".class"))
}

fun HTTPServer.log(text: String) = println(currentTime() + " HTTP >> ".blue() + text)
