package org.kotlinnative.translator.llvm.types

class LLVMVoidType() : LLVMType() {

    override fun getAlign(): Int = 0
    override fun toString(): String = "void"

}
