package org.kotlinnative.translator.llvm.types

class LLVMStringType(val length : Int) : LLVMType() {
    override val size: Byte = 1
    override val align = 8
    override val defaultValue = ""

    override fun toString(): String = "i8*"
    fun fullType() = "[${length+1} x i8]"
}
