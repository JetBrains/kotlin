/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMIsDeclaration
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.driver.phases.OptimizationState
import org.jetbrains.kotlin.backend.konan.llvm.LlvmFunctionAttribute
import org.jetbrains.kotlin.backend.konan.llvm.addLlvmFunctionEnumAttribute
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import org.jetbrains.kotlin.backend.konan.llvm.name
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
                .filter { LLVMIsDeclaration(it) == 0 && it.name != "__clang_call_terminate" }
                .forEach { addLlvmFunctionEnumAttribute(it, sspAttribute) }
    }
}