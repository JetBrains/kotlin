package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.exceptions.UnimplementedException
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue


class LLVMIntType() : LLVMType() {

    override val align = 4
    override var size: Int = 4
    override val defaultValue = "0"
    override val mangle = "Int"
    override val typename = "i32"
    override val isPrimitive = true

    override fun convertFrom(source: LLVMSingleValue) =
            when (source.type!!) {
                is LLVMBooleanType,
                is LLVMByteType,
                is LLVMCharType,
                is LLVMShortType -> LLVMExpression(LLVMBooleanType(), " sext ${source.type} $source to i32")
                else -> throw UnimplementedException()
            }

    override fun operatorOr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "or i32 $firstOp, $secondOp")

    override fun operatorAnd(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "and i32 $firstOp, $secondOp")

    override fun operatorXor(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "xor i32 $firstOp, $secondOp")

    override fun operatorShl(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "shl i32 $firstOp, $secondOp")

    override fun operatorShr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "ashr i32 $firstOp, $secondOp")

    override fun operatorUshr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "lshr i32 $firstOp, $secondOp")

    override fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "sub nsw i32 $firstOp, $secondOp")

    override fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "mul nsw i32 $firstOp, $secondOp")

    override fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "add nsw i32 $firstOp, $secondOp")

    override fun operatorDiv(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "sdiv i32 $firstOp, $secondOp")

    override fun operatorInc(firstOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "add nsw i32 $firstOp, 1")

    override fun operatorDec(firstOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "sub nsw i32 $firstOp, 1")

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp slt i32 $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sgt i32 $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sle i32 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sge i32 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp eq i32" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp ne i32" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorMod(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMIntType(), "srem i32 $firstOp, $secondOp")

    override fun equals(other: Any?) =
            other is LLVMIntType

    override fun hashCode() =
            mangle.hashCode()

}