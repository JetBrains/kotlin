package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable


class LLVMIntType() : LLVMType() {

    //TODO switch by types: int + double = int
    override fun operatorMinus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression =
            LLVMExpression(LLVMIntType(), "sub nsw i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorTimes(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression =
            LLVMExpression(LLVMIntType(), "mul nsw i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorPlus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression =
            LLVMExpression(LLVMIntType(), "add nsw i32 $firstOp, $secondOp")

    override fun toString() = "i32"

    override fun getAlign() = 4
}