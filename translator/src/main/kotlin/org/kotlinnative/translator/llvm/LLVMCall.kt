package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMCall(val returnType: LLVMType, val name: String, val arguments: List<LLVMVariable>) : LLVMNode() {

    override fun toString(): String {
        return "call $returnType $name(${arguments.joinToString { "${it.type} ${it.label}" }})"
    }
}
