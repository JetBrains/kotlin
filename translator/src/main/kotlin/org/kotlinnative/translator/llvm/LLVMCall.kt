package org.kotlinnative.translator.llvm

class LLVMCall(val returnType: String, val name: String, val arguments: List<LLVMVariable>) : LLVMNode() {

    override fun toString(): String {
        return "call $returnType $name(${arguments.joinToString { "${it.type} ${it.label}" }})"
    }
}
