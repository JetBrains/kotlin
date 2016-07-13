package org.kotlinnative.translator.llvm

class LLVMLabel(val label: String) : LLVMNode() {

    override fun toString(): String = label
}