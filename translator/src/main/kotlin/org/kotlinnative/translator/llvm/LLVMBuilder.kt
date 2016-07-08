package org.kotlin.native.translator.llvm

class LLVMBuilder {
    private val llvmCode: StringBuilder = StringBuilder()

    fun addLlvmCode(code: String) {
        llvmCode.appendln(code)
    }

    override fun toString(): String {
        return llvmCode.toString()
    }
}