package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue
import org.kotlinnative.translator.llvm.addAfterIfNotEmpty
import java.util.*

class LLVMReferenceType(val type: String,
                        var prefix: String = "",
                        override var align: Int = TranslationState.pointerAlign,
                        override var size: Int = TranslationState.pointerSize,
                        var byRef: Boolean = true) : LLVMType() {

    override val defaultValue: String = "null"
    override val mangle = "Ref_$type"
    override val typename = prefix.addAfterIfNotEmpty(".") + type

    override fun toString() = "%$typename"

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp eq ${firstOp.getType()} $firstOp, ${if (secondOp.type is LLVMNullType) "null" else "$secondOp"}")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp ne ${firstOp.getType()} $firstOp, ${if (secondOp.type is LLVMNullType) "null" else "$secondOp"}")

    override fun equals(other: Any?) =
            (other is LLVMReferenceType) and (typename.equals((other as LLVMReferenceType).typename))

    override fun hashCode() =
            typename.hashCode()
}