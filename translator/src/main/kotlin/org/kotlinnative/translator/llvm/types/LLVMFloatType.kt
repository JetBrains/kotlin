package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue


class LLVMFloatType() : LLVMType() {

    override val align = 4
    override var size: Int = 4
    override val mangle = "Float"
    override val typename = "float"
    override val defaultValue = "0.0"
    override val isPrimitive = true

    override fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMFloatType(), "fsub float $firstOp, $secondOp")

    override fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMFloatType(), "fmul float $firstOp, $secondOp")

    override fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMFloatType(), "fadd float $firstOp, $secondOp")

    override fun operatorDiv(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMFloatType(), "fdiv float $firstOp, $secondOp")

    override fun operatorInc(firstOp: LLVMSingleValue) =
            LLVMExpression(LLVMDoubleType(), "fadd float $firstOp, 1.0")

    override fun operatorDec(firstOp: LLVMSingleValue) =
            LLVMExpression(LLVMDoubleType(), "fsub float $firstOp, 1.0")

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp olt float $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp ogt float $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp ole float i32 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp oge float i32 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp oeq float" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMBooleanType(), "fcmp one float" + (if ((firstOp.pointer > 0) || (secondOp.pointer > 0)) "*" else "") + " $firstOp, $secondOp")

    override fun operatorMod(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue) =
            LLVMExpression(LLVMFloatType(), "frem float $firstOp, $secondOp")

    override fun equals(other: Any?) =
            other is LLVMFloatType

    override fun hashCode() =
            typename.hashCode()

}