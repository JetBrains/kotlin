/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.toKString
import llvm.LLVMGetValueName
import llvm.LLVMIsAFunction
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.driver.phases.OptimizationState
import org.jetbrains.kotlin.backend.konan.llvm.LlvmFunctionAttribute
import org.jetbrains.kotlin.backend.konan.llvm.addLlvmFunctionEnumAttribute
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import kotlin.sequences.forEach

enum class StackProtectorMode {
    NO, YES, STRONG, ALL;

    val attribute
        get() = when (this) {
            NO -> null
            YES -> LlvmFunctionAttribute.Ssp
            STRONG -> LlvmFunctionAttribute.SspStrong
            ALL -> LlvmFunctionAttribute.SspReq
        }
}

internal fun applySspAttributes(context: OptimizationState, module: LLVMModuleRef) {
    context.llvmConfig.sspMode.attribute?.let { sspAttribute ->
        getFunctions(module)
                .filter { LLVMIsAFunction(it) != null && LLVMGetValueName(it)?.toKString() != "__clang_call_terminate" }
                .forEach { addLlvmFunctionEnumAttribute(it, sspAttribute) }
    }
}