package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue
import java.util.*

class LLVMReferenceType(val type: String, var prefix: String = "", override var align: Int = 4, override var size: Int = 4, var byRef: Boolean = true) : LLVMType() {

    override val defaultValue: String = ""

    override val typename: String
        get() = "$prefix${if (prefix.length > 0) "." else ""}${
        if (location.size > 0) "${location.joinToString(".")}." else ""
        }$type"

    override fun toString() = "%$typename"

    private val params = ArrayList<String>()

    val location = ArrayList<String>()

    override fun mangle() = "Ref_$type"

    fun addParam(param: String) {
        params.add(param)
    }

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp eq ${firstOp.getType()} $firstOp, ${if (secondOp.type is LLVMNullType) "null" else "$secondOp"}")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp ne ${firstOp.getType()} $firstOp, ${if (secondOp.type is LLVMNullType) "null" else "$secondOp"}")

    override fun equals(other: Any?): Boolean {
        return (other is LLVMReferenceType) and (typename.equals((other as LLVMReferenceType).typename))
    }

    override fun hashCode() =
            typename.hashCode()
}