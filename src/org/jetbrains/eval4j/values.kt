package org.jetbrains.eval4j

import org.objectweb.asm.Type
import org.objectweb.asm.tree.LabelNode

trait Value : org.objectweb.asm.tree.analysis.Value {
    val asmType: Type
    val valid: Boolean
    override fun getSize(): Int = asmType.getSize()
}

abstract class AbstractValue(
        override val asmType: Type
) : Value {
    override val valid = true
}

object NOT_A_VALUE: Value {
    override val asmType = Type.getType("<invalid>")
    override val valid = false
    override fun getSize(): Int = 1
}

class NotInitialized(override val asmType: Type): Value {
    override val valid = false
}

class IntValue(val value: Int, asmType: Type): AbstractValue(asmType)
class LongValue(val value: Long): AbstractValue(Type.LONG_TYPE)
class FloatValue(val value: Float): AbstractValue(Type.FLOAT_TYPE)
class DoubleValue(val value: Double): AbstractValue(Type.DOUBLE_TYPE)
class ObjectValue(val value: Any?, asmType: Type): AbstractValue(asmType)

class LabelValue(val value: LabelNode): AbstractValue(Type.VOID_TYPE)

fun boolean(v: Boolean) = IntValue(if (v) 1 else 0, Type.BOOLEAN_TYPE)
fun byte(v: Byte) = IntValue(v.toInt(), Type.BYTE_TYPE)
fun short(v: Short) = IntValue(v.toInt(), Type.SHORT_TYPE)
fun char(v: Char) = IntValue(v.toInt(), Type.CHAR_TYPE)
fun int(v: Int) = IntValue(v, Type.INT_TYPE)
fun long(v: Long) = LongValue(v)
fun float(v: Float) = FloatValue(v)
fun double(v: Double) = DoubleValue(v)
fun obj<T>(v: T, t: Type = if (v != null) Type.getType(v.javaClass) else Type.getType(javaClass<Any>())) = ObjectValue(v, t)

val NULL_VALUE = ObjectValue(null, Type.getObjectType("null"))

val Value.boolean: Boolean
    get(): Boolean {
        assert(this.asmType == Type.BOOLEAN_TYPE)
        return (this as IntValue).value == 1
    }

val Value.int: Int get() = (this as IntValue).value
val Value.long: Long get() = (this as LongValue).value
val Value.float: Float get() = (this as FloatValue).value
val Value.double: Double get() = (this as DoubleValue).value
val Value.obj: Any? get() = (this as ObjectValue).value