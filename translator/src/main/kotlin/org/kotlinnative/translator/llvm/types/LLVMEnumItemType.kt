package org.kotlinnative.translator.llvm.types


class LLVMEnumItemType() : LLVMType() {

    override val align = 4
    override val size: Byte = 4
    override val defaultValue = "0"

    override fun toString() = "i32"
}