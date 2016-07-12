package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.exceptions.UnimplementedException
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue
import org.kotlinnative.translator.llvm.LLVMVariable

abstract class LLVMType() : Cloneable {

    open fun operatorPlus(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorTimes(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorMinus(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorLt(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorGt(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorLeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorGeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorEq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorNeq(result: LLVMVariable, firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()

    fun makeClone() = clone()

    abstract val align: Int
    abstract val size: Byte
}

fun parseLLVMType(type: String): LLVMType = when (type) {
    "i32" -> LLVMIntType()
    "i16" -> LLVMShortType()
    "i8" -> LLVMCharType()
    "Unit" -> LLVMVoidType()
    else -> LLVMReferenceType(type)
}
