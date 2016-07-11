package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMVariable(val label: String, val type: LLVMType?) : LLVMNode() {

    override fun toString(): String {
        return label
    }
}