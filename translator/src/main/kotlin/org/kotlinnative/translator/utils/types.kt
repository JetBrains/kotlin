package org.kotlinnative.translator.utils

import org.kotlinnative.translator.llvm.types.LLVMType

data class FunctionArgument(val type: LLVMType, val name: String)

data class FunctionDescriptor(val returnType: LLVMType, val argTypes: List<LLVMType>)
