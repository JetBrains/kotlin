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
import llvm.LLVMDisposeMessage
import llvm.LLVMKotlinEmitObjectFile
import llvm.LLVMModuleRef
import llvm.LLVMRelocMode
import llvm.LLVMTargetMachineRef
import org.jetbrains.kotlin.backend.konan.llvm.LlvmBackendContext
import org.jetbrains.kotlin.backend.konan.llvm.withLlvmBackendContext
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * Uses the in-process LLVM code generator to compile the bitcode contained in an LLVM module.
 * It runs the same code generation pipeline as `clang -cc1 -emit-obj` (see `LLVMKotlinEmitObjectFile`).
 *
 * [llvmBackendContext] is borrowed: the target machine it provides is owned by the LLVM backend job
 * that also ran the bitcode post-processing on the module.
 *
 * When the relocation mode is not specified for the output, `clang -cc1` generates position-independent code,
 * while the target machine of [llvmBackendContext] uses the default model of the target (e.g. static for x86_64 Linux
 * executables). Such modules are emitted with a target machine that only differs in generating position-independent code,
 * so that the object file is the same as the one clang produces.
 */
internal class LlvmBitcodeCompiler(
        private val llvmBackendContext: LlvmBackendContext,
) : BitcodeCompiler {

    override fun makeObjectFile(llvmModule: LLVMModuleRef, outputFilePath: Path) {
        val config = llvmBackendContext.config
        if (config.targetSpec.relocMode == LLVMRelocMode.LLVMRelocDefault) {
            val clangDefault = config.copy(targetSpec = config.targetSpec.copy(relocMode = LLVMRelocMode.LLVMRelocPIC))
            withLlvmBackendContext(clangDefault) { emit(targetMachine, llvmModule, outputFilePath) }
        } else {
            emit(llvmBackendContext.targetMachine, llvmModule, outputFilePath)
        }
    }

    private fun emit(targetMachine: LLVMTargetMachineRef, llvmModule: LLVMModuleRef, outputFilePath: Path) = memScoped {
        val llvmError = alloc<CPointerVar<ByteVar>>()
        val emissionFailed = LLVMKotlinEmitObjectFile(
                targetMachine,
                llvmModule,
                outputFilePath.absolutePathString(),
                llvmError.ptr,
        ) != 0
        if (emissionFailed) {
            val message = llvmError.value?.toKString()
            LLVMDisposeMessage(llvmError.value)
            error("Failed to emit LLVM module to object file $outputFilePath: $message")
        }
    }
}
