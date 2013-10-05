package org.jetbrains.eval4j

import org.objectweb.asm.tree.analysis.*
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.Label

class UnsupportedByteCodeException(message: String) : RuntimeException(message)

trait EvalStrategy {
    fun loadClass(classType: Type): Value
    fun newInstance(classType: Type): Value
    fun checkCast(value: Value, targetType: Type): Value
    fun isInsetanceOf(value: Value, targetType: Type): Boolean

    fun newArray(arrayType: Type, size: Int): Value
    fun getArrayLength(array: Value): Value

    fun getStaticField(fieldDesc: String): Value
    fun setStaticField(fieldDesc: String, newValue: Value)

    fun getField(instance: Value, fieldDesc: String): Value
    fun setField(instance: Value, fieldDesc: String, newValue: Value)

    fun getLocalVariable(index: Int): Value
    fun setLocalVariable(index: Int, newValue: Value)

    fun jump(label: Label)

    fun returnValue(value: Value)
    fun throwException(value: Value)
}

class EvalInterpreter(private val evalStrategy: EvalStrategy) : Interpreter<Value>(ASM4) {
    override fun newValue(`type`: Type?): Value? {
        if (`type` == null) {
            return NOT_A_VALUE
        }

        return when (`type`.getSort()) {
            Type.VOID -> null
            else -> NotInitialized(`type`)
        }
    }

    override fun newOperation(insn: AbstractInsnNode): Value? {
        return when (insn.getOpcode()) {
            ACONST_NULL -> {
                return NULL_VALUE
            }

            ICONST_M1 -> int(-1)
            ICONST_0 -> int(0)
            ICONST_1 -> int(1)
            ICONST_2 -> int(2)
            ICONST_3 -> int(3)
            ICONST_4 -> int(4)
            ICONST_5 -> int(5)

            LCONST_0 -> long(0)
            LCONST_1 -> long(1)

            FCONST_0 -> float(0.0)
            FCONST_1 -> float(1.0)
            FCONST_2 -> float(2.0)

            DCONST_0 -> double(0.0)
            DCONST_1 -> double(1.0)

            BIPUSH, SIPUSH -> int((insn as IntInsnNode).operand)

            LDC -> {
                val cst = ((insn as LdcInsnNode)).cst
                when (cst) {
                    is Int -> int(cst)
                    is Float -> float(cst)
                    is Long -> long(cst)
                    is Double -> double(cst)
                    is String -> obj(cst)
                    is Type -> {
                        val sort = (cst as Type).getSort()
                        when (sort) {
                            Type.OBJECT, Type.ARRAY -> evalStrategy.loadClass(cst)
                            Type.METHOD -> throw UnsupportedByteCodeException("Mothod handles are not supported")
                            else -> throw UnsupportedByteCodeException("Illegal LDC constant " + cst)
                        }
                    }
                    is Handle -> throw UnsupportedByteCodeException("Method handles are not supported")
                    else -> throw UnsupportedByteCodeException("Illegal LDC constant " + cst)
                }
            }
            JSR -> LabelValue((insn as JumpInsnNode).label.getLabel())
            GETSTATIC -> evalStrategy.getStaticField((insn as FieldInsnNode).desc)
            NEW -> evalStrategy.newInstance(Type.getType((insn as TypeInsnNode).desc))
            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: Value): Value {
        return value
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: Value): Value? {
        return when (insn.getOpcode()) {
            INEG -> int(-value.int)
            IINC -> {
                val varIndex = (insn as IincInsnNode).`var`
                evalStrategy.setLocalVariable(
                        varIndex,
                        int(evalStrategy.getLocalVariable(varIndex).int + insn.incr)
                )

                // TODO: don't know what to do here, returning what we got:
                value
            }
            L2I -> int(value.long.toInt())
            F2I -> int(value.float.toInt())
            D2I -> int(value.double.toInt())
            I2B -> byte(value.int.toByte())
            I2C -> char(value.int.toChar())
            I2S -> short(value.int.toShort())

            FNEG -> float(-value.float)
            I2F -> float(value.int.toFloat())
            L2F -> float(value.long.toFloat())
            D2F -> float(value.double.toFloat())

            LNEG -> long(-value.long)
            I2L -> long(value.int.toLong())
            F2L -> long(value.float.toLong())
            D2L -> long(value.double.toLong())

            DNEG -> double(-value.double)
            I2D -> double(value.int.toDouble())
            L2D -> double(value.long.toDouble())
            F2D -> double(value.float.toDouble())

            IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> {
                val label = (insn as JumpInsnNode).label.getLabel()
                when (insn.getOpcode()) {
                    IFEQ -> if (value.int == 0) evalStrategy.jump(label)
                    IFNE -> if (value.int != 0) evalStrategy.jump(label)
                    IFLT -> if (value.int < 0) evalStrategy.jump(label)
                    IFGT -> if (value.int > 0) evalStrategy.jump(label)
                    IFLE -> if (value.int <= 0) evalStrategy.jump(label)
                    IFGE -> if (value.int >= 0) evalStrategy.jump(label)
                    IFNULL -> if (value.obj == null) evalStrategy.jump(label)
                    IFNONNULL -> if (value.obj != null) evalStrategy.jump(label)
                }
                null
            }

            // TODO: switch
            TABLESWITCH,
            LOOKUPSWITCH -> throw UnsupportedByteCodeException("Switch is not supported yet")

            IRETURN,
            LRETURN,
            FRETURN,
            DRETURN,
            ARETURN -> {
                evalStrategy.returnValue(value)
                null
            }

            PUTSTATIC -> {
                evalStrategy.setStaticField((insn as FieldInsnNode).desc, value)
                null
            }

            GETFIELD -> evalStrategy.getField(value, (insn as FieldInsnNode).desc)

            NEWARRAY -> {
                val typeStr = when ((insn as IntInsnNode).operand) {
                    T_BOOLEAN -> "[Z"
                    T_CHAR    -> "[C"
                    T_BYTE    -> "[B"
                    T_SHORT   -> "[S"
                    T_INT     -> "[I"
                    T_FLOAT   -> "[F"
                    T_DOUBLE  -> "[D"
                    T_LONG    -> "[J"
                    else -> throw AnalyzerException(insn, "Invalid array type")
                }
                evalStrategy.newArray(Type.getType(typeStr), value.int)
            }
            ANEWARRAY -> {
                val desc = (insn as TypeInsnNode).desc
                evalStrategy.newArray(Type.getType("[" + Type.getObjectType(desc)), value.int)
            }
            ARRAYLENGTH -> evalStrategy.getArrayLength(value)

            ATHROW -> {
                evalStrategy.throwException(value)
                null
            }

            CHECKCAST -> {
                val targetType = Type.getObjectType((insn as TypeInsnNode).desc)
                evalStrategy.checkCast(value, targetType)
            }

            INSTANCEOF -> {
                val targetType = Type.getObjectType((insn as TypeInsnNode).desc)
                boolean(evalStrategy.isInsetanceOf(value, targetType))
            }

            // TODO: maybe just do nothing?
            MONITORENTER, MONITOREXIT -> throw UnsupportedByteCodeException("Monitor instructions are not supported")

            else -> throw UnsupportedByteCodeException("$insn")
        }
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: Value, value2: Value): Value {
        when (insn.getOpcode()) {
            IALOAD, BALOAD, CALOAD, SALOAD, IADD, ISUB, IMUL, IDIV, IREM, ISHL, ISHR, IUSHR, IAND, IOR, IXOR -> {
                return Value.INT_VALUE
            }
            FALOAD, FADD, FSUB, FMUL, FDIV, FREM -> {
                return Value.FLOAT_VALUE
            }
            LALOAD, LADD, LSUB, LMUL, LDIV, LREM, LSHL, LSHR, LUSHR, LAND, LOR, LXOR -> {
                return Value.LONG_VALUE
            }
            DALOAD, DADD, DSUB, DMUL, DDIV, DREM -> {
                return Value.DOUBLE_VALUE
            }
            AALOAD -> {
                return Value.REFERENCE_VALUE
            }
            LCMP, FCMPL, FCMPG, DCMPL, DCMPG -> {
                return Value.INT_VALUE
            }
            IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, PUTFIELD -> {
                return null
            }
            else -> {
                throw Error("Internal error.")
            }
        }
    }

    override fun ternaryOperation(insn: AbstractInsnNode, value1: Value, value2: Value?, value3: Value): Value? {
        return null
    }

    override fun naryOperation(insn: AbstractInsnNode, values: List<out Value>): Value {
        val opcode = insn.getOpcode()!!
        if (opcode == MULTIANEWARRAY) {
            return newValue(Type.getType(((insn as MultiANewArrayInsnNode)).desc))
        }
        else if (opcode == INVOKEDYNAMIC) {
            return newValue(Type.getReturnType(((insn as InvokeDynamicInsnNode)).desc))
        }
        else {
            return newValue(Type.getReturnType(((insn as MethodInsnNode)).desc))
        }
    }


    override fun returnOperation(insn: AbstractInsnNode?, value: Value, expected: Value?) {

    }

    override fun merge(v: Value, w: Value): Value {
        if (!v.equals(w)) {
            return Value.UNINITIALIZED_VALUE
        }

        return v
    }
}
