package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.exceptions.UnimplementedException
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue


class LLVMLongType() : LLVMType() {

    override val align = 8
    override var size = 8
    override val defaultValue = "0"
    override val typename = "i64"
    override val mangle = "Long"
    override val isPrimitive = true

    override fun convertFrom(source: LLVMSingleValue) =
            when (source.type!!) {
                is LLVMBooleanType,
                is LLVMByteType,
                is LLVMCharType,
                is LLVMShortType,
                is LLVMIntType -> LLVMExpression(LLVMBooleanType(), "  sext ${source.type} $source to i64")
                else -> throw UnimplementedException()
            }

    override fun operatorOr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "or i64 $firstOp, $secondOp")

    override fun operatorAnd(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "and i64 $firstOp, $secondOp")

    override fun operatorXor(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "xor i64 $firstOp, $secondOp")

    override fun operatorShl(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "shl i64 $firstOp, $secondOp")

    override fun operatorShr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "ashr i64 $firstOp, $secondOp")

    override fun operatorUshr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "lshr i64 $firstOp, $secondOp")

    override fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "sub nsw i64 $firstOp, $secondOp")

    override fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "mul nsw i64 $firstOp, $secondOp")

    override fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "add nsw i64 $firstOp, $secondOp")

    override fun operatorDiv(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "sdiv i64 $firstOp, $secondOp")

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp slt i64 $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sgt i64 $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sle i64 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp sge i64 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp eq i64" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "icmp ne i64" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorMod(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMLongType(), "srem i64 $firstOp, $secondOp")

    override fun equals(other: Any?) =
            other is LLVMLongType

    override fun hashCode() =
            mangle.hashCode()

}