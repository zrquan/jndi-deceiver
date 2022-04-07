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

    private fun log(text: String) = println("LDAP >> ".purple() + text)

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
        log("Listening on ${Options.address}:$port".blue())
    }

    override fun processSearchResult(result: InMemoryInterceptedSearchResult) {
        val base = result.request.baseDN
        val key = base.substringBefore("/")

        log("Receive a request to $base")

        // key 和方法名一致
        when (key) {
            "ref" -> ref(result, base)
            "tomcat" -> tomcat(result, base)
            "groovy" -> groovy(result, base)
            else -> log("Payload not found: $key".red())
        }
    }
}
