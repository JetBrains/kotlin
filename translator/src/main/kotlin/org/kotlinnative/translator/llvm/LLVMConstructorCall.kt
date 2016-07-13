package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMConstructorCall(override val type: LLVMType, val call: (LLVMVariable) -> LLVMCall) : LLVMSingleValue()

