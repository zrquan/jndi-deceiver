package ldap

import Options
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor
import ldap.controllers.Controller
import ldap.controllers.LDAPMapping
import org.reflections.Reflections
import util.blue
import util.currentTime
import util.red
import java.net.InetAddress
import javax.net.ServerSocketFactory
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

class LDAPServer : InMemoryOperationInterceptor() {
    private val port = Options.ldapPort

    val routes = mutableMapOf<String, Controller>()

    init {
        // find all classes annotated with @LDAPMapping
        val controllers = Reflections(this::class.java.`package`.name)
            .getTypesAnnotatedWith(LDAPMapping::class.java)
        controllers.forEach {
            val cons = it.getConstructor()
            val instance = cons.newInstance() as Controller
            val mappings = it.getAnnotation(LDAPMapping::class.java).uri

            for (map in mappings) {
                if (map.startsWith("/")) {
                    routes[map.substring(1)] = instance
                } else {
                    routes[map] = instance
                }
            }
        }
    }

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
        println("[LDAP] Listening on 0.0.0.0:$port".blue())
    }

    override fun processSearchResult(result: InMemoryInterceptedSearchResult) {
        val base = result.request.baseDN
        var controller: Controller? = null

        println("${currentTime()} [LDAP] Receive a request to $base")

        for (key in routes.keys) {
            if (key.equals(base) ||
                key.endsWith("*") && base.startsWith(key.substring(0, key.length - 1))) {
                controller = routes[key]
                break
            }
        }

        if (controller != null) {
            controller.sendResult(result, base)
        } else {
            println("${currentTime()} [LDAP] Payload $base not found".red())
        }
    }
}
