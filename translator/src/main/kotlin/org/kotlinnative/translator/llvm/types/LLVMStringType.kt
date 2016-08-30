package org.kotlinnative.translator.llvm.types

class LLVMStringType(override val length: Int, var isLoaded: Boolean = true) : LLVMArray, LLVMType() {

    override val align = 8
    override var size: Int = 1
    override val defaultValue = ""
    override val typename = "i8*"
    override val mangle = "String"
    override val arrayElementType = LLVMCharType()
    override val fullArrayType = "[${length + 1} x i8]"

    override fun equals(other: Any?) =
            when (other) {
                is LLVMStringType -> this.length == other.length
                else -> false
            }

    override fun hashCode() =
            length * 31 + if (isLoaded) 1 else 0 +
                    mangle.hashCode()

}