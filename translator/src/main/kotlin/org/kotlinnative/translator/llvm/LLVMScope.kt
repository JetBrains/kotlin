package org.kotlinnative.translator.llvm

open class LLVMScope

class LLVMGlobalScope : LLVMScope() {
    override fun toString() = "@"
}

class LLVMLocalScope : LLVMScope() {
    override fun toString() = "%"
}

