package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue
import org.kotlinnative.translator.llvm.LLVMVariable


class LLVMIntType() : LLVMType() {

    //TODO switch by types: int + double = int
    override fun operatorMinus(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "sub nsw i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorTimes(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "mul nsw i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorPlus(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "add nsw i32 $firstOp, $secondOp")

    override fun operatorLt(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp slt i32 $firstOp, $secondOp")

    override fun operatorGt(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp sgt i32 $firstOp, $secondOp")

    override fun operatorLeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp sle i32 $firstOp, $secondOp")

    override fun operatorGeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp sge i32 $firstOp, $secondOp")

    override fun operatorEq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp eq i32 $firstOp, $secondOp")

    override fun operatorNeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp ne i32 $firstOp, $secondOp")

    override val align = 4
    override val size: Byte = 4

    override fun toString() = "i32"
}