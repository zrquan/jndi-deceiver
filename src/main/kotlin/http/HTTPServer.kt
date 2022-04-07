package http

import util.Options
import io.javalin.Javalin
import util.blue
import util.purple

class HTTPServer {
    private val port = Options.httpPort

    private fun log(text: String) = println("HTTP >> ".purple() + text)

    fun run() {
        val app = Javalin.create().start(port)
        log("Listening on ${Options.address}:$port".blue())

        app.get("/*") { ctx ->
            val uri = ctx.req.requestURI
            log("Receive a request to $uri")
            ctx.result(getPayload(uri.substring(1)))
        }
    }

    private fun getPayload(filename: String): ByteArray =
        PayloadGenerator().generate(filename.substringBefore(".class"))
}
