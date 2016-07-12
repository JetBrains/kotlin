package org.kotlinnative.translator.llvm.types

class LLVMShortType() : LLVMType() {

    override val size: Byte = 2
    override val align = 1

    override fun toString(): String = "i16"
}
