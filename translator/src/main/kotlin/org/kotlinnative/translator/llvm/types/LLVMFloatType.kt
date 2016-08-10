package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue


class LLVMFloatType() : LLVMType() {

    //TODO switch by types: int + double = int
    override fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMFloatType(), "fsub float i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMFloatType(), "fmul float i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMFloatType(), "fadd float $firstOp, $secondOp")

    override fun equals(other: Any?): Boolean {
        return other is LLVMFloatType
    }

    override val align = 4
    override var size: Int = 4

    override fun mangle() = "Float"
    override fun toString() = "float"
    override val defaultValue = "0.0"
    override fun isPrimitive() = true
}