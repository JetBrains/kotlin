/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import llvm.LLVMCodeGenFileType
import llvm.LLVMDisposeMessage
import llvm.LLVMModuleRef
import llvm.LLVMKotlinEmitObjectFiles
import llvm.LLVMGetErrorMessage
import llvm.LLVMDisposeErrorMessage
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

    override fun makeObjectFiles(llvmModule: LLVMModuleRef, outputPaths: List<Path>) {
        if (outputPaths.size == 1) {
            makeObjectFile(llvmModule, outputPaths.single())
            return
        }
        memScoped {
            val paths = allocArray<CPointerVar<ByteVar>>(outputPaths.size)
            outputPaths.forEachIndexed { index, path -> paths[index] = path.absolutePathString().cstr.getPointer(this) }
            val failure = LLVMKotlinEmitObjectFiles(llvmModule, llvmBackendContext.targetMachine, outputPaths.size, paths)
            if (failure != null) {
                val message = LLVMGetErrorMessage(failure)!!
                try {
                    error("Failed to emit object files: ${message.toKString()}")
                } finally {
                    LLVMDisposeErrorMessage(message)
                }
            }
        }
    }

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
