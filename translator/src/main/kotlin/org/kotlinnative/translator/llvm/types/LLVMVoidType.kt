package org.kotlinnative.translator.llvm.types

class LLVMVoidType() : LLVMType() {

    override val align = 0
    override var size = 0
    override val defaultValue = ""
    override val typename = "void"

    override fun mangle() = ""

    override fun equals(other: Any?) =
            other is LLVMVoidType

    override fun hashCode() =
            typename.hashCode()

}