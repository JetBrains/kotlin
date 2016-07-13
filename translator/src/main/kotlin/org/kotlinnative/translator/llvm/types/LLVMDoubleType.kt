package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue


class LLVMDoubleType() : LLVMType() {

    //TODO switch by types: int + double = int
    override fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fsub double i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fmul double i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fadd double $firstOp, $secondOp")

    override val align = 8
    override val size: Byte = 8
    override fun toString() = "double"
}