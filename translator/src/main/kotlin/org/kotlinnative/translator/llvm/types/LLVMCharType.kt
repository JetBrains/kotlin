package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue

class LLVMCharType() : LLVMType() {

    override val align = 1
    override var size: Int = 1
    override val mangle = "Char"
    override val typename = "i8"
    override val defaultValue = "0"
    override val isPrimitive = true

    override fun parseArg(inputArg: String) = inputArg.first().toInt().toString()

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp slt i8 $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sgt i8 $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sle i8 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sge i8 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp eq i8" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp ne i8" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun equals(other: Any?) =
            other is LLVMCharType

    override fun hashCode() =
            mangle.hashCode()

}