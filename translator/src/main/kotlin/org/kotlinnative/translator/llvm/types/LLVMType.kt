package org.kotlinnative.translator.llvm.types;

import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.exceptions.UnimplementedException
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

abstract class LLVMType() {

    open fun operatorPlus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression = throw UnimplementedException()
    open fun operatorTimes(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression = throw UnimplementedException()
    open fun operatorMinus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression = throw UnimplementedException()

    abstract fun getAlign(): Int
}

fun parseLLVMType(type: String): LLVMType = when (type) {
    "i32" -> LLVMIntType()
    "Unit" -> LLVMVoidType()
    else -> throw TranslationException()
}
