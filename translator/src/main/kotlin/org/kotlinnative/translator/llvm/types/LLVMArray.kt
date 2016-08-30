package org.kotlinnative.translator.llvm.types

interface LLVMArray {

    val arrayElementType: LLVMType
    val fullArrayType: String
    val length: Int

}