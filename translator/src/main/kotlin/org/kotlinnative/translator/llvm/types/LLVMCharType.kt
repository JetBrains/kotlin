package org.kotlinnative.translator.llvm.types

class LLVMCharType() : LLVMType() {

    override val align = 1
    override val size: Byte = 1
    override fun toString(): String = "i8"
}
