package org.kotlinnative.translator.llvm.types

class LLVMReferenceType(val type: String) : LLVMType() {

    override val align = 4
    override val size: Byte = 4

    override fun toString() = type

}