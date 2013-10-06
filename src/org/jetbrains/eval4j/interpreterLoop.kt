package org.jetbrains.eval4j

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Type
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.util.Printer
import org.objectweb.asm.tree.TryCatchBlockNode
import java.util.ArrayList

trait InterpreterResult {
    fun toString(): String
}

class ExceptionThrown(val exception: Value): InterpreterResult {
    override fun toString(): String = "Thrown $exception"
}

data class ValueReturned(val result: Value): InterpreterResult {
    override fun toString(): String = "Returned $result"
}

class AbnormalTermination(val message: String): InterpreterResult {
    override fun toString(): String = "Terminated abnormally: $message"
}

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
    if (firstInsn == null) throw IllegalArgumentException("Empty method")

    var currentInsn = firstInsn!!

    fun goto(nextInsn: AbstractInsnNode?) {
        if (nextInsn == null) throw IllegalArgumentException("Instruction flow ended with no RETURN")
        currentInsn = nextInsn
    }

    val interpreter = SingleInstructionInterpreter(eval)
    val frame = initFrame(ownerClassInternalName, m, interpreter)
    val handlers = computeHandlers(m)

    fun exceptionCaught(exceptionValue: Value): Boolean {
        val catchBlocks = handlers[m.instructions.indexOf(currentInsn)] ?: listOf()
        for (catch in catchBlocks) {
            val exceptionTypeInternalName = catch.`type`
            if (exceptionTypeInternalName != null) {
                val exceptionType = Type.getObjectType(exceptionTypeInternalName)
                if (eval.isInstanceOf(exceptionValue, exceptionType)) {
                    frame.clearStack()
                    frame.push(exceptionValue)
                    goto(catch.handler)
                    return true
                }
            }
        }
        return false
    }

    while (true) {
        val insnOpcode = currentInsn.getOpcode()
        val insnType = currentInsn.getType()

        when (insnType) {
            AbstractInsnNode.LABEL,
            AbstractInsnNode.FRAME,
            AbstractInsnNode.LINE -> {
                // skip to the next instruction
            }

            else -> {
                when (insnOpcode) {
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

                    IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> {
                        val value = frame.getStack(0)!!
                        val expectedType = Type.getReturnType(m.desc)
                        if (expectedType.getSort() == Type.OBJECT) {
                            val coerced = if (value != NULL_VALUE && value.asmType != expectedType)
                                                ObjectValue(value.obj, expectedType)
                                          else value
                            return ValueReturned(coerced)
                        }
                        if (value.asmType != expectedType) {
                            assert(insnOpcode == IRETURN, "Only ints should be coerced: " + Printer.OPCODES[insnOpcode])

                            val coerced = when (expectedType.getSort()) {
                                Type.BOOLEAN -> boolean(value.boolean)
                                Type.BYTE -> byte(value.int.toByte())
                                Type.SHORT -> short(value.int.toShort())
                                Type.CHAR -> char(value.int.toChar())
                                else -> throw UnsupportedByteCodeException("Should not be coerced: $expectedType")
                            }
                            return ValueReturned(coerced)
                        }
                        return ValueReturned(value)
                    }
                    RETURN -> return ValueReturned(VOID_VALUE)
                    IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> {
                        if (interpreter.checkUnaryCondition(frame.getStack(0)!!, insnOpcode)) {
                            frame.execute(currentInsn, interpreter)
                            goto((currentInsn as JumpInsnNode).label)
                            continue
                        }
                    }
                    IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> {
                        if (interpreter.checkBinaryCondition(frame.getStack(0)!!, frame.getStack(1)!!, insnOpcode)) {
                            frame.execute(currentInsn, interpreter)
                            goto((currentInsn as JumpInsnNode).label)
                            continue
                        }
                    }

                    ATHROW -> {
                        val exceptionValue = frame.getStack(0)!!
                        val handled = handler.exceptionThrown(frame, currentInsn, exceptionValue)
                        if (handled != null) return handled
                        if (exceptionCaught(exceptionValue)) continue
                        return ExceptionThrown(exceptionValue)
                    }

                    // Workaround for a bug in Kotlin: NoPatterMatched exception is thrown otherwise!
                    else -> {}
                }

                try {
                    frame.execute(currentInsn, interpreter)
                }
                catch (e: ThrownFromEvalException) {
                    val handled = handler.exceptionThrown(frame, currentInsn, e.exception)
                    if (handled != null) return handled
                    if (exceptionCaught(e.exception)) continue
                    return ExceptionThrown(e.exception)
                }
            }
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

fun computeHandlers(m: MethodNode): Array<out List<TryCatchBlockNode>?> {
    val insns = m.instructions
    val handlers = Array<MutableList<TryCatchBlockNode>?>(insns.size()) {null}
    for (tcb in m.tryCatchBlocks) {
        val begin = insns.indexOf(tcb.start)
        val end = insns.indexOf(tcb.end)
        for (i in begin..end - 1) {
            val insnHandlers = handlers[i] ?: ArrayList<TryCatchBlockNode>()
            handlers[i] = insnHandlers

            insnHandlers.add(tcb)
        }
    }

    return handlers
}