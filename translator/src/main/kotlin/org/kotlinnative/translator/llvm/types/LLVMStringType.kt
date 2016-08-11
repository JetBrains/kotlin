package org.kotlinnative.translator.llvm.types

class LLVMStringType(override val length: Int) : LLVMArray, LLVMType() {

    override var size: Int = 1
    override val align = 8
    override val defaultValue = ""

    override fun mangle() = "String"

    override fun equals(other: Any?): Boolean =
            when (other) {
                is LLVMStringType -> this.length == other.length
                else -> false
            }

    override fun basicType() = LLVMCharType()
    override val typename = "i8*"
    override fun fullType() = "[${length + 1} x i8]"
}
