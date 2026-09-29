/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import llvm.LLVMCodeGenFileType
import llvm.LLVMDisposeMessage
import llvm.LLVMModuleRef
import llvm.LLVMTargetMachineEmitToFile
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * Uses the in-process LLVM compiler to compile the bitcode contained in a LLVM module.
 */
internal class LlvmBitcodeCompiler(
        private val context: BitcodePostProcessingContext,
) : BitcodeCompiler<LLVMModuleRef> {

    override fun makeObjectFile(bitcodeContainer: LLVMModuleRef, outputFilePath: Path) = memScoped {
        // For Apple targets, it is preferred using `GlobalSel`, that should be the default
        // for aarch64 (at least, when optimizations are disable).
        // Source: https://github.com/Kotlin/llvm-project/blob/a5124ee2ea70e57b3e4601d9596cab75f9ab796b/llvm/lib/Target/AArch64/AArch64TargetMachine.cpp#L403
        val llvmError = alloc<CPointerVar<ByteVar>>()
        val emissionSuccess = LLVMTargetMachineEmitToFile(
                context.llvmBridge.targetMachine,
                bitcodeContainer,
                outputFilePath.absolutePathString(),
                LLVMCodeGenFileType.LLVMObjectFile,
                llvmError.ptr
        ) == 0
        if (!emissionSuccess) {
            val message = llvmError.value?.toKString()
            LLVMDisposeMessage(llvmError.value)
            error("Failed to emit LLVM bitcode to object file: ${message}")
        }
    }
}