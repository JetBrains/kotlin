package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMVariable(val label: String,
                        type: LLVMType,
                        var kotlinName: String? = null,
                        val scope: LLVMScope = LLVMRegisterScope(),
                        pointer: Int = 0) : LLVMSingleValue(type, pointer) {

    override fun toString(): String = "$scope$label"

}