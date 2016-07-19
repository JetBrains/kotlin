package org.kotlinnative.translator.llvm.types

class LLVMNullType() : LLVMType() {
    override val align: Int = 0
    override var size: Int = 0
    override val defaultValue: String = "0"
}
