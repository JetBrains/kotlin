package org.kotlinnative.translator.llvm.types

interface LLVMArray {

    fun basicType(): LLVMType
    fun fullType(): String

    val length: Int
}