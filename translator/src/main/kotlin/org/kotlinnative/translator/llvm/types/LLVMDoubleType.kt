package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue


class LLVMDoubleType() : LLVMType() {

    //TODO switch by types: int + double = int
    override fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fsub double i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fmul double i32 $firstOp, $secondOp")

    //TODO switch by types: int + double = int
    override fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fadd double $firstOp, $secondOp")

    override fun operatorInc(firstOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fadd double $firstOp, 1.0")

    override fun operatorDec(firstOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMDoubleType(), "fsub double $firstOp, 1.0")

    override fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "fcmp olt double $firstOp, $secondOp")

    override fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "fcmp ogt double $firstOp, $secondOp")

    override fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "fcmp ole double i32 $firstOp, $secondOp")

    override fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "fcmp oge double i32 $firstOp, $secondOp")

    override fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "fcmp oeq double $firstOp, $secondOp")

    override fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression =
            LLVMExpression(LLVMBooleanType(), "fcmp one double $firstOp, $secondOp")

    override fun equals(other: Any?): Boolean {
        return other is LLVMDoubleType
    }

    override fun mangle() = "Double"

    override val align = 8
    override var size: Int = 8
    override val typename = "double"
    override val defaultValue = "0.0"
    override fun isPrimitive() = true
    override fun hashCode() =
            mangle().hashCode()

}