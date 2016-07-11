package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMExpression(val variableType: LLVMType, val llvmCode: String) : LLVMNode() {

    override fun toString(): String {
        return llvmCode
    }

}