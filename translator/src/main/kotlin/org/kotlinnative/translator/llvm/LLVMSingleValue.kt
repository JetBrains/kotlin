package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMSingleValue(val type: LLVMType, var pointer: Int = 0) : LLVMNode() {

    open val pointedType: String
            get() = type.toString() + "*".repeat(pointer)

}