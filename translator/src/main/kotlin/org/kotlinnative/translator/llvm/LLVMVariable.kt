package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMVariable(val label: String, override val type: LLVMType, var kotlinName: String? = null, val scope: LLVMScope = LLVMLocalScope(), override var pointer: Int = 0) : LLVMSingleValue() {

    override fun getType(): String = type.toString() + "*".repeat(pointer)

    override fun toString(): String = "$scope$label"
}