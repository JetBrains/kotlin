package org.kotlinnative.translator.llvm.types;

import org.kotlinnative.translator.exceptions.TranslationException
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMVariable

abstract class LLVMType() {
    abstract fun operatorPlus(result: LLVMVariable, firstOp: LLVMVariable, secondOp: LLVMVariable): LLVMExpression;
}

fun parseLLVMType(type: String): LLVMType = when(type) {
    "i32" -> LLVMIntType()
    else -> throw TranslationException()
}
