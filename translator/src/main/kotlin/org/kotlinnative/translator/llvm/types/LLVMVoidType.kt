package org.kotlinnative.translator.llvm.types

class LLVMVoidType() : LLVMType() {

    override val align = 0
    override val size: Byte = 0
    override val defaultValue = ""

    override fun toString(): String = "void"
}
