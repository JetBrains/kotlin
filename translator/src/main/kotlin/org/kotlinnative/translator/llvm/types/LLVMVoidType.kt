package org.kotlinnative.translator.llvm.types

class LLVMVoidType() : LLVMType() {

    override val align = 0

    override fun toString(): String = "void"
    override val size: Byte = 0

}
