package org.jetbrains.eval4j

import org.objectweb.asm.Type
import org.objectweb.asm.tree.LabelNode

trait Value : org.objectweb.asm.tree.analysis.Value {
    val asmType: Type
    val valid: Boolean
    override fun getSize(): Int = asmType.getSize()

    override fun toString(): String
}

object NOT_A_VALUE: Value {
    override val asmType = Type.getType("<invalid>")
    override val valid = false
    override fun getSize(): Int = 1

    override fun toString() = "NOT_A_VALUE"
}

object VOID_VALUE: Value {
    override val asmType: Type = Type.VOID_TYPE
    override val valid: Boolean = false
    override fun toString() = "VOID_VALUE"
}

fun makeNotInitializedValue(t: Type): Value? {
    return when (t.getSort()) {
        Type.VOID -> null
        else -> NotInitialized(t)
    }
}

class NotInitialized(override val asmType: Type): Value {
    override val valid = false
    override fun toString() = "NotInitialized: $asmType"
}

abstract class AbstractValueBase<V>(
        override val asmType: Type
) : Value {
    override val valid = true
    abstract val value: V

    override fun toString() = "$value: $asmType"

    override fun equals(other: Any?): Boolean {
        if (other !is AbstractValue<*>) return false

        return value == other.value && asmType == other.asmType
    }

    override fun hashCode(): Int {
        return value.hashCode() + 17 * asmType.hashCode()
    }
}

abstract class AbstractValue<V>(
        override val value: V,
        asmType: Type
) : AbstractValueBase<V>(asmType)

class IntValue(value: Int, asmType: Type): AbstractValue<Int>(value, asmType)
class LongValue(value: Long): AbstractValue<Long>(value, Type.LONG_TYPE)
class FloatValue(value: Float): AbstractValue<Float>(value, Type.FLOAT_TYPE)
class DoubleValue(value: Double): AbstractValue<Double>(value, Type.DOUBLE_TYPE)
class ObjectValue(value: Any?, asmType: Type): AbstractValue<Any?>(value, asmType)
class NewObjectValue(asmType: Type): AbstractValueBase<Any?>(asmType) {
    override var value: Any? = null
}

class LabelValue(value: LabelNode): AbstractValue<LabelNode>(value, Type.VOID_TYPE)

fun boolean(v: Boolean) = IntValue(if (v) 1 else 0, Type.BOOLEAN_TYPE)
fun byte(v: Byte) = IntValue(v.toInt(), Type.BYTE_TYPE)
fun short(v: Short) = IntValue(v.toInt(), Type.SHORT_TYPE)
fun char(v: Char) = IntValue(v.toInt(), Type.CHAR_TYPE)
fun int(v: Int) = IntValue(v, Type.INT_TYPE)
fun long(v: Long) = LongValue(v)
fun float(v: Float) = FloatValue(v)
fun double(v: Double) = DoubleValue(v)
//fun obj<T>(v: T, t: Type = if (v != null) Type.getType(v.javaClass) else Type.getType(javaClass<Any>())) = ObjectValue(v, t)

val NULL_VALUE = ObjectValue(null, Type.getObjectType("null"))

val Value.boolean: Boolean get() = (this as IntValue).value == 1
val Value.int: Int get() = (this as IntValue).value
val Value.long: Long get() = (this as LongValue).value
val Value.float: Float get() = (this as FloatValue).value
val Value.double: Double get() = (this as DoubleValue).value
val Value.obj: Any?
    get(): Any? {
        if (this is NewObjectValue) {
            val v = value
            if (v == null) throw IllegalStateException("Trying to access an unitialized object: $this")
            return v
        }
        return (this as AbstractValue<*>).value
    }

fun Any?.checkNull(): Any {
    if (this == null) {
        throwException(NullPointerException())
    }
    return this
}

fun throwException(e: Throwable): Nothing {
    throw ThrownFromEvalException(ObjectValue(e, Type.getType(e.javaClass)))
}