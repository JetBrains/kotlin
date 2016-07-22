package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue


class LLVMLongType() : LLVMType() {

    override fun operatorOr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMLongType(), "or i64 $firstOp, $secondOp")

    override fun operatorAnd(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMLongType(), "and i64 $firstOp, $secondOp")

    override fun operatorXor(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMLongType(), "xor i64 $firstOp, $secondOp")

    override fun operatorShl(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMLongType(), "shl i64 $firstOp, $secondOp")

    override fun operatorShr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMLongType(), "ashr i64 $firstOp, $secondOp")

    override fun operatorUshr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMLongType(), "lshr i64 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMLongType(), "sub nsw i64 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMLongType(), "mul nsw i64 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMLongType(), "add nsw i64 $firstOp, $secondOp")

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp slt i64 $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp sgt i64 $firstOp, $secondOp")


    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp sle i64 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp sge i64 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp eq i64 $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "icmp ne i64 $firstOp, $secondOp")

    override val align = 4
    override var size: Int = 8
    override val defaultValue = "0"

    override fun mangle() = "Long"
    override fun toString() = "i64"
    override fun isPrimitive() = true
}