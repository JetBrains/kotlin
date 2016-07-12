package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable


class LLVMDoubleType() : LLVMType() {

    //TODO switch by types: int + double = int
    override fun operatorMinus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fsub double i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorTimes(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fmul double i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorPlus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fadd double $firstOp, $secondOp")

    override fun toString() = "double"

    override val align = 8
}