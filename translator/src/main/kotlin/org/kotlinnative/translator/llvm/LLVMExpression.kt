package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMExpression(val variableType: LLVMType, val llvmCode: String, val pointer: Int = 0) : LLVMNode() {

    override fun toString() = llvmCode

}