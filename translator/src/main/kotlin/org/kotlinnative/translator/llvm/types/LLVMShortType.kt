package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue
import org.kotlinnative.translator.llvm.LLVMVariable

class LLVMShortType() : LLVMType() {
    override fun operatorLt(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp slt i16 $firstOp, $secondOp")

    override fun operatorGt(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp sgt i16 $firstOp, $secondOp")

    override fun operatorLeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp sle i16 $firstOp, $secondOp")

    override fun operatorGeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp sge i16 $firstOp, $secondOp")

    override fun operatorEq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp eq i16 $firstOp, $secondOp")

    override fun operatorNeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "icmp ne i16 $firstOp, $secondOp")

    override val size: Byte = 2
    override val align = 2
    override fun toString(): String = "i16"
}
