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
import org.jetbrains.kotlin.backend.konan.llvm.LlvmBackendContext
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * Uses the in-process LLVM code generator to compile the bitcode contained in an LLVM module.
 *
 * [llvmBackendContext] is borrowed: the target machine it provides is owned by the LLVM backend job
 * that also ran the bitcode post-processing on the module.
 */
internal class LlvmBitcodeCompiler(
        private val llvmBackendContext: LlvmBackendContext,
) : BitcodeCompiler {

    override fun makeObjectFile(llvmModule: LLVMModuleRef, outputFilePath: Path) = memScoped {
        val llvmError = alloc<CPointerVar<ByteVar>>()
        val emissionFailed = LLVMTargetMachineEmitToFile(
                llvmBackendContext.targetMachine,
                llvmModule,
                outputFilePath.absolutePathString(),
                LLVMCodeGenFileType.LLVMObjectFile,
                llvmError.ptr,
        ) != 0
        if (emissionFailed) {
            val message = llvmError.value?.toKString()
            LLVMDisposeMessage(llvmError.value)
            error("Failed to emit LLVM module to object file $outputFilePath: $message")
        }
    }
}
