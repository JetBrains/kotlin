package org.kotlinnative.translator.llvm.types


class LLVMEnumItemType() : LLVMType() {

    override val align = 4
    override var size: Int = 4
    override val defaultValue = "0"

    override fun toString() = "i32"
}