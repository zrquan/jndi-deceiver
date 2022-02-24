package http.payloads

import jdk.internal.org.objectweb.asm.Opcodes.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.reflections.Reflections


class Payload(var version: Int) {
    val routes = mutableMapOf<String, Class<*>>()

    init {
        // only support java 7 or 8 now
        version = when (version) {
            7, V1_7 -> V1_7
            else -> V1_8
        }

        // setup routes
        val payloads = Reflections(this::class.java.`package`.name)
            .getTypesAnnotatedWith(HTTPMapping::class.java)
        payloads.forEach {
            val mappings = it.getAnnotation(HTTPMapping::class.java).uri

            for (map in mappings) {
                if (map.startsWith("/")) {
                    routes[map.substring(1)] = it
                } else {
                    routes[map] = it
                }
            }
        }
    }

    fun generate(mapping: String): ByteArray {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        // 类名要和文件名对上
        cw.visit(version, ACC_PUBLIC or ACC_SUPER, mapping, null, "java/lang/Object", null)

        cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).run {
            visitCode()
            val label0 = Label()
            visitLabel(label0)
            visitLineNumber(6, label0)
            visitVarInsn(ALOAD, 0)
            visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            visitInsn(RETURN)
            val label1 = Label()
            visitLabel(label1)
            visitLocalVariable("this", "LTemplate;", null, label0, label1, 0)
            visitMaxs(1, 1)
            visitEnd()
        }

        cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null).run {
            val mv = dispatch(mapping, this)
            mv.visitCode()
            visitInsn(RETURN)
            visitMaxs(1, 0)
            visitEnd()
        }
        cw.visitEnd()

        return cw.toByteArray()
    }

    private fun dispatch(key: String, mv: MethodVisitor): MethodVisitor {
        var payload: MethodVisitor = mv
        routes.forEach { (t, u) ->
            if (t.equals(key) ||
                t.endsWith("*") && t.startsWith(key.substring(0, t.length - 1))
            ) {
                payload = u.getConstructor(MethodVisitor::class.java).newInstance(mv) as MethodVisitor
                return@forEach
            }
        }
        return payload
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HTTPMapping(val uri: Array<String>)
