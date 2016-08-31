package org.kotlinnative.translator.llvm

class LLVMLabel(val label: String, val scope: LLVMScope) : LLVMNode() {

    override fun toString() = "$scope$label"

}