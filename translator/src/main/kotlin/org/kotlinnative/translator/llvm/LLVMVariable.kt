package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMVariable(val label: String, val type: LLVMType? = null, var kotlinName: String? = null, var pointer: Boolean = false) : LLVMNode() {

    fun getType(): String = type.toString() + if (pointer) "*" else ""

    override fun toString(): String = label
}