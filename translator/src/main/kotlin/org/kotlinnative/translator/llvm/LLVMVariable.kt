package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMVariable(val label: String,
                        override val type: LLVMType,
                        var kotlinName: String? = null,
                        val scope: LLVMScope = LLVMRegisterScope(),
                        pointer: Int = 0) : LLVMSingleValue(type, pointer) {

    override fun getType(): String = type.toString() + "*".repeat(pointer)

    override fun toString(): String = "$scope$label"

}