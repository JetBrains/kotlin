package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMCharType() : LLVMType() {
    override fun operatorLt(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp slt i8 $firstOp, $secondOp")

    override fun operatorGt(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp sgt i8 $firstOp, $secondOp")

    override fun operatorLeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp sle i8 $firstOp, $secondOp")

    override fun operatorGeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp sge i8 $firstOp, $secondOp")

    override fun operatorEq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp eq i8 $firstOp, $secondOp")

    override fun operatorNeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp ne i8 $firstOp, $secondOp")

    override val align = 1
    override val size: Byte = 1
    override fun toString(): String = "i8"
}
