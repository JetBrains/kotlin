package org.kotlinnative.translator.llvm

data class LLVMVariable(val label: String){
    override fun toString(): String {
        return label
    }
}