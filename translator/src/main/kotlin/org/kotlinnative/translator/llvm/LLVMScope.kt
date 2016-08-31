package org.kotlinnative.translator.llvm

abstract class LLVMScope

class LLVMVariableScope : LLVMScope() {
    override fun toString() = "@"
}

class LLVMRegisterScope : LLVMScope() {
    override fun toString() = "%"
}