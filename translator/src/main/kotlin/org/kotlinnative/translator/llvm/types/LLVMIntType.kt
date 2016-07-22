package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue


class LLVMIntType() : LLVMType() {

    override fun operatorOr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "or i32 $firstOp, $secondOp")

    override fun operatorAnd(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "and i32 $firstOp, $secondOp")

    override fun operatorXor(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "xor i32 $firstOp, $secondOp")

    override fun operatorShl(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "shl i32 $firstOp, $secondOp")

    override fun operatorShr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "ashr i32 $firstOp, $secondOp")

    override fun operatorUshr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "lshr i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "sub nsw i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "mul nsw i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMIntType(), "add nsw i32 $firstOp, $secondOp")

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp slt i32 $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression {
        return LLVMExpression(LLVMBooleanType(), "icmp sgt i32 $firstOp, $secondOp")
    }

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp sle i32 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp sge i32 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp eq i32 $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp ne i32 $firstOp, $secondOp")

    override fun mangle() = "Int"

    override val align = 4
    override var size: Int = 4
    override val defaultValue = "0"

    override fun toString() = "i32"
    override fun isPrimitive() = true
}