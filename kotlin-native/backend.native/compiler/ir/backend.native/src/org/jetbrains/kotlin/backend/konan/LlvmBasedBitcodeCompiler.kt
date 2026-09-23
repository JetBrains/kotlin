/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.Arena
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import llvm.LLVMCodeGenFileType
import llvm.LLVMCodeGenOptLevel
import llvm.LLVMCodeModel
import llvm.LLVMCreateTargetMachine
import llvm.LLVMGetTargetFromTriple
import llvm.LLVMModuleRef
import llvm.LLVMTargetMachineEmitToFile
import llvm.LLVMTargetRefVar
import org.jetbrains.kotlin.backend.konan.driver.cpuFeatures
import org.jetbrains.kotlin.backend.konan.driver.cpuModel
import java.io.Closeable
import java.io.File

internal class LlvmBasedBitcodeCompiler(
        private val context: NativeGenerationState,
) : BitcodeCompiler<LLVMModuleRef>, Closeable {

    companion object {
        init {
            LlvmOptimizationPipeline.initLLVMOnce()
        }
    }

    private val arena = Arena()

    private val targetMachine = run {
        val optimizationLevel = if (context.shouldOptimize()) LLVMCodeGenOptLevel.LLVMCodeGenLevelDefault else LLVMCodeGenOptLevel.LLVMCodeGenLevelNone
        val codeModel = LLVMCodeModel.LLVMCodeModelDefault
        val targetTriple = context.llvm.targetTriple
        val relocMode = context.config.platform.configurables.currentRelocationMode(context).toLlvmRelocMode()
        val cpuModel = context.cpuModel
        val cpuFeatures = context.cpuFeatures
        val target = arena.alloc<LLVMTargetRefVar>()
        val foundLlvmTarget = LLVMGetTargetFromTriple(targetTriple, target.ptr, null) == 0
        check(foundLlvmTarget) { "Cannot get target from triple ${targetTriple}." }
        LLVMCreateTargetMachine(
                target.value,
                targetTriple,
                cpuModel,
                cpuFeatures,
                optimizationLevel,
                relocMode,
                codeModel
        )!!
    }

    override fun makeObjectFile(bitcodeContainer: LLVMModuleRef, outputObjectFile: File) {
        val error = arena.alloc<CPointerVar<ByteVar>>()
        val result = LLVMTargetMachineEmitToFile(
                targetMachine,
                bitcodeContainer,
                outputObjectFile.absolutePath,
                LLVMCodeGenFileType.LLVMObjectFile,
                error.ptr
        ) == 0
        if (!result) {
            context.log { "Error during bitcode generation:\n${error.value!!.toKString()}" }
            throw IllegalStateException("Error during bitcode generation: ${error.value!!.toKString()}")
        }
    }

    override fun close() {
        arena.clear()
    }
}