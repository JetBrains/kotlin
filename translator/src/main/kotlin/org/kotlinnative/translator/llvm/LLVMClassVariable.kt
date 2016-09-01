package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.llvm.types.LLVMType

class LLVMClassVariable(label: String,
                        type: LLVMType,
                        pointer: Int = 0,
                        var offset: Int = 0) : LLVMVariable(label, type, pointer = pointer)