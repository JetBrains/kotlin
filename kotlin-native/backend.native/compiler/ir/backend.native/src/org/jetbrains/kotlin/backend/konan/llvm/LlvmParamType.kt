/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef

/**
 * LLVM function's parameter type with its attributes.
 */
class LlvmParamType(val llvmType: LLVMTypeRef, val attributes: List<LlvmParameterAttribute> = emptyList())

/**
 * LLVM function's return type with its attributes.
 */
class LlvmRetType(val llvmType: LLVMTypeRef, val attributes: List<LlvmParameterAttribute> = emptyList(), val isObjectType: Boolean = false)
