package org.jetbrains.eval4j.jdi

import org.jetbrains.eval4j.*
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Type
import org.objectweb.asm.Opcodes.*
import com.sun.jdi

fun makeInitialFrame(methodNode: MethodNode, arguments: List<Value>): Frame<Value> {
    val isStatic = (methodNode.access and ACC_STATIC) != 0
    assert(isStatic, "Instance methods are not supported: $methodNode")

    val params = Type.getArgumentTypes(methodNode.desc)
    assert(params.size == arguments.size(), "Wrong number of arguments for $methodNode: $arguments")

    val frame = Frame<Value>(methodNode.maxLocals, methodNode.maxStack)
    frame.setReturn(makeNotInitializedValue(Type.getReturnType(methodNode.desc)))

    for ((i, arg) in arguments.withIndices()) {
        frame.setLocal(i, arg)
    }

    for (i in arguments.size..methodNode.maxLocals - 1) {
        frame.setLocal(i, NOT_A_VALUE)
    }
    
    return frame
}

class JDIFailureException(message: String?, cause: Throwable? = null): RuntimeException(message, cause)

fun <T: Any> T?.sure(message: String? = null): T = this ?: throw JDIFailureException(message)

fun jdi.Value?.asValue(): Value {
    return when (this) {
        null -> NULL_VALUE
        is jdi.VoidValue -> VOID_VALUE
        is jdi.BooleanValue -> IntValue(intValue(), Type.BOOLEAN_TYPE)
        is jdi.ByteValue -> IntValue(intValue(), Type.BYTE_TYPE)
        is jdi.ShortValue -> IntValue(intValue(), Type.SHORT_TYPE)
        is jdi.CharValue -> IntValue(intValue(), Type.CHAR_TYPE)
        is jdi.IntegerValue -> IntValue(intValue(), Type.INT_TYPE)
        is jdi.LongValue -> LongValue(longValue())
        is jdi.FloatValue -> FloatValue(floatValue())
        is jdi.DoubleValue -> DoubleValue(doubleValue())
        is jdi.ObjectReference -> ObjectValue(this, `type`().asType())
        else -> throw JDIFailureException("Unknown value: $this")
    }
}

fun jdi.Type.asType(): Type = Type.getType(this.signature())

val Value.jdiObj: jdi.ObjectReference?
    get() = this.obj as jdi.ObjectReference?

val Value.jdiClass: jdi.ClassObjectReference?
    get() = this.jdiObj as jdi.ClassObjectReference?

fun Value.asJdiValue(vm: jdi.VirtualMachine): jdi.Value? {
    return when (this) {
        NULL_VALUE -> null
        VOID_VALUE -> vm.mirrorOfVoid()
        is IntValue -> when (asmType) {
            Type.BOOLEAN_TYPE -> vm.mirrorOf(boolean)
            Type.BYTE_TYPE -> vm.mirrorOf(int.toByte())
            Type.SHORT_TYPE -> vm.mirrorOf(int.toShort())
            Type.CHAR_TYPE -> vm.mirrorOf(int.toChar())
            Type.INT_TYPE -> vm.mirrorOf(int)
            else -> throw JDIFailureException("Unknown value type: $this")
        }
        is LongValue -> vm.mirrorOf(value)
        is FloatValue -> vm.mirrorOf(value)
        is DoubleValue -> vm.mirrorOf(value)
        is ObjectValue -> value as jdi.ObjectReference
        is NewObjectValue -> throw JDIFailureException("Illegal value: $this")
        else -> throw JDIFailureException("Unknown value: $this")
    }
}