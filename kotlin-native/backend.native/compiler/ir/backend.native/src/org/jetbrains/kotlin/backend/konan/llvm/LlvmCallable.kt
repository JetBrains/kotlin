/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMGetReturnType
import llvm.LLVMTypeRef
import llvm.LLVMValueRef

/**
 *  Wrapper around LLVM value of functional type.
 */
class LlvmCallable(val llvmValue: LLVMValueRef, val attributeProvider: LlvmFunctionAttributeProvider) {
    val returnType: LLVMTypeRef by lazy {
        LLVMGetReturnType(functionType)!!
    }

    val functionType: LLVMTypeRef by lazy {
        getFunctionType(llvmValue)
    }
}