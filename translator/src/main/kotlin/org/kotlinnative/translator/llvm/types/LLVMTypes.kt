package org.kotlinnative.translator.llvm.types

//TODO skeleton for typeList

enum class LLVMTypes {
    int, double, float, char
}

val typesMap = mapOf(Pair(LLVMTypes.int, ::LLVMIntType));
