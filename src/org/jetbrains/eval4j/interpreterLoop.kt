package org.jetbrains.eval4j

import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Type
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.VarInsnNode

trait InterpreterResult
object NOTHING_DONE: InterpreterResult
class ExceptionThrown(val exception: Value): InterpreterResult
class ValueReturned(val result: Value): InterpreterResult
class AbnormalTermination(val message: String): InterpreterResult

trait InterpretationEventHandler {

    class object {
        object NONE : InterpretationEventHandler {
            override fun instructionProcessed(insn: AbstractInsnNode): InterpreterResult? = null
            override fun exceptionThrown(currentState: Frame<Value>, currentInsn: AbstractInsnNode, exception: Value): InterpreterResult? = null
            override fun exceptionCaught(currentState: Frame<Value>, currentInsn: AbstractInsnNode, exception: Value): InterpreterResult? = null
        }
    }

    // If a non-null value is returned, interpreter loop is terminated and that value is used as a result
    fun instructionProcessed(insn: AbstractInsnNode): InterpreterResult?

    fun exceptionThrown(currentState: Frame<Value>, currentInsn: AbstractInsnNode, exception: Value): InterpreterResult?
    fun exceptionCaught(currentState: Frame<Value>, currentInsn: AbstractInsnNode, exception: Value): InterpreterResult?
}

class ThrownFromEvalException(val exception: Value): RuntimeException()

fun interpreterLoop(
        ownerClassInternalName: String,
        m: MethodNode,
        eval: Eval,
        handler: InterpretationEventHandler = InterpretationEventHandler.NONE
): InterpreterResult {
    val firstInsn = m.instructions.getFirst()
    if (firstInsn == null) return NOTHING_DONE

    var currentInsn = firstInsn!!

    fun goto(nextInsn: AbstractInsnNode?) {
        if (nextInsn == null) throw IllegalArgumentException("Instruction flow ended with no RETURN")
        currentInsn = nextInsn
    }

    val interpreter = SingleInstructionInterpreter(eval)
        var frame = initFrame(ownerClassInternalName, m, interpreter)

    while (true) {
        // TODO try-catch-finally support

        val insnOpcode = currentInsn.getOpcode()
        val insnType = currentInsn.getType()

        when (insnType) {
            AbstractInsnNode.LABEL,
            AbstractInsnNode.FRAME,
            AbstractInsnNode.LINE -> {
                // skip to the next instruction
            }

            else -> when (insnOpcode) {
                GOTO -> {
                    goto((currentInsn as JumpInsnNode).label)
                    continue
                }

                RET -> {
                    val varNode = currentInsn as VarInsnNode
                    val address = frame.getLocal(varNode.`var`)
                    goto((address as LabelValue).value)
                    continue
                }

                // TODO: switch
                LOOKUPSWITCH -> UnsupportedByteCodeException("LOOKUPSWITCH is not supported yet")
                TABLESWITCH -> UnsupportedByteCodeException("TABLESWITCH is not supported yet")

                IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> return ValueReturned(frame.getStack(0)!!)
                RETURN -> return ValueReturned(VOID_VALUE)
                IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> {
                    if (interpreter.checkUnaryCondition(frame.getStack(0)!!, insnOpcode)) {
                        goto((currentInsn as JumpInsnNode).label)
                        continue
                    }
                }
                IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> {
                    if (interpreter.checkBinaryCondition(frame.getStack(0)!!, frame.getStack(1)!!, insnOpcode)) {
                        goto((currentInsn as JumpInsnNode).label)
                        continue
                    }
                }

                // TODO: try/catch/finally
                ATHROW -> {
                    val exceptionValue = frame.getStack(0)!!
                    val handled = handler.exceptionThrown(frame, currentInsn, exceptionValue)
                    if (handled != null) return handled
                    return ExceptionThrown(exceptionValue)
                }
            }
        }

        try {
            frame.execute(currentInsn, interpreter)
        }
        catch (e: ThrownFromEvalException) {
            // TODO: try/catch.finaly
            val handled = handler.exceptionThrown(frame, currentInsn, e.exception)
            if (handled != null) return handled
            return ExceptionThrown(e.exception)
        }

        val handled = handler.instructionProcessed(currentInsn)
        if (handled != null) return handled

        goto(currentInsn.getNext())
    }
}

// Copied from org.objectweb.asm.tree.analysis.Analyzer.analyze()
fun <V : org.objectweb.asm.tree.analysis.Value> initFrame(
        owner: String,
        m: MethodNode,
        interpreter: Interpreter<V>
): Frame<V> {
    val current = Frame<V>(m.maxLocals, m.maxStack)
    current.setReturn(interpreter.newValue(Type.getReturnType(m.desc)))

    var local = 0
    if ((m.access and ACC_STATIC) == 0) {
        val ctype = Type.getObjectType(owner)
        current.setLocal(local++, interpreter.newValue(ctype))
    }

    val args = Type.getArgumentTypes(m.desc)
    for (i in 0..args.size - 1) {
        current.setLocal(local++, interpreter.newValue(args[i]))
        if (args[i].getSize() == 2) {
            current.setLocal(local++, interpreter.newValue(null))
        }
    }

    while (local < m.maxLocals) {
        current.setLocal(local++, interpreter.newValue(null))
    }

    return current
}