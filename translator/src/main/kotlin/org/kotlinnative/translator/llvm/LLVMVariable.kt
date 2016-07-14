package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMVariable(val label: String, override val type: LLVMType, var kotlinName: String? = null, val scope: LLVMScope = LLVMLocalScope(), override var pointer: Boolean = false) : LLVMSingleValue() {

    override fun getType(): String = type.toString() + if (pointer) "*" else ""

    override fun toString(): String = "$scope$label"
}