/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState

private val dataLayoutMismatch = Regex("""^linking two modules of different data layouts: .* is '([^']*)' whereas .* is '([^']*)'""", RegexOption.IGNORE_CASE)

internal fun llvmLinkModules2(generationState: NativeGenerationState, dest: LLVMModuleRef, src: LLVMModuleRef): LLVMBool {
    val diagnosticHandler = DefaultLlvmDiagnosticHandler(generationState, generationState.diagnosticReporter, object : DefaultLlvmDiagnosticHandler.Policy {
        override fun suppressWarning(diagnostic: LlvmDiagnostic): Boolean {
            if (super.suppressWarning(diagnostic)) return true

            // Workaround https://youtrack.jetbrains.com/issue/KT-35001.
            // Note: SDK version mismatch is generally harmless.
            // Also it is expected: LLVM bitcode can be built in different environments with different SDK versions,
            // and then linked by the compiler altogether.
            // Just ignore such warnings for now:
            if (diagnostic.message.startsWith("linking module flags 'SDK Version': IDs have conflicting values")) return true

            val [sourceLayout, destinationLayout] = dataLayoutMismatch.find(diagnostic.message)?.destructured ?: return false
            return sourceLayout.removeSuffix("-ni:1") == destinationLayout.removeSuffix("-ni:1")
        }
    })

    return withLlvmDiagnosticHandler(generationState.llvmContext, diagnosticHandler) {
        LLVMLinkModules2(dest, src)
    }
}
