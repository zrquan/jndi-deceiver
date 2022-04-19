package http

import util.Option
import io.javalin.Javalin
import payload.PayloadGenerator
import util.blue
import util.purple

class HTTPServer {
    private val port = Option.httpPort

    private fun log(text: String) = println("HTTP >> ".purple() + text)

    fun run() {
        val app = Javalin.create().start(port)
        log("Listening on ${Option.address}:$port".blue())

        app.get("/*") { ctx ->
            val uri = ctx.req.requestURI
            log("Receive a request to $uri")
            ctx.result(getPayload(uri.substring(1)))
        }
    }

    private fun getPayload(filename: String): ByteArray =
        PayloadGenerator().generate(filename.substringBefore(".class"))
}
