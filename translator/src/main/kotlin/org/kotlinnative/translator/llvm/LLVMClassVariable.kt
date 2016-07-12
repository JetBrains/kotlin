package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType


class LLVMClassVariable(label: String, type: LLVMType? = null, offset: Int = 0) : LLVMVariable(label, type)
