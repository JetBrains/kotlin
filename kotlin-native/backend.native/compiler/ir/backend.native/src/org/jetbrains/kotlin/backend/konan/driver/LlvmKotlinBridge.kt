/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import llvm.LLVMCodeGenOptLevel
import llvm.LLVMCodeModel
import llvm.LLVMCreateTargetMachine
import llvm.LLVMDisposeMessage
import llvm.LLVMDisposeTargetMachine
import llvm.LLVMGetTargetFromTriple
import llvm.LLVMKotlinInitializeTargets
import llvm.LLVMRelocMode
import llvm.LLVMTargetMachineRef
import llvm.LLVMTargetRefVar
import llvm.loadLLVMStubs
import org.jetbrains.kotlin.backend.konan.NativeBackendDiagnostics
import org.jetbrains.kotlin.backend.konan.currentRelocationMode
import org.jetbrains.kotlin.backend.konan.toLlvmRelocMode
import org.jetbrains.kotlin.konan.target.Configurables

internal val NativeBackendPhaseContext.cpuModel: String
    get() {
        val target = config.target
        val configurables: Configurables = config.platform.configurables
        return configurables.targetCpu ?: run {
            this@cpuModel.diagnosticReporter.report(NativeBackendDiagnostics.LLVM_WARNING, "targetCpu for target $target was not set. Targeting `generic` cpu.")
            "generic"
        }
    }

internal val NativeBackendPhaseContext.cpuFeatures: String
    get() = config.platform.configurables.targetCpuFeatures ?: ""

internal val NativeBackendPhaseContext.llvmOptimizationLevel: LLVMCodeGenOptLevel
    get() = when {
        config.optimizationsEnabled -> LLVMCodeGenOptLevel.LLVMCodeGenLevelAggressive   // clangOptFlags:   -O3
        config.debug -> LLVMCodeGenOptLevel.LLVMCodeGenLevelNone                        // clangDebugFlags: -O0
        else -> LLVMCodeGenOptLevel.LLVMCodeGenLevelLess                                // clangNooptFlags: -O1
    }

internal val NativeBackendPhaseContext.llvmRelocMode: LLVMRelocMode
    get() = config.platform.configurables.currentRelocationMode(this).toLlvmRelocMode()

internal data class TargetMachineConfig(
        val targetTriple: String,
        val cpuModel: String,
        val cpuFeatures: String,
        val optLevel: LLVMCodeGenOptLevel,
        val relocMode: LLVMRelocMode,
        val codeModel: LLVMCodeModel = LLVMCodeModel.LLVMCodeModelDefault,
)

internal fun NativeBackendPhaseContext.createTargetMachineConfig(targetTriple: String) = TargetMachineConfig(
        targetTriple = targetTriple,
        cpuModel = cpuModel,
        cpuFeatures = cpuFeatures,
        optLevel = llvmOptimizationLevel,
        relocMode = llvmRelocMode,
)

/**
 * Owns the native LLVM objects used for in-process code generation.
 * It is owned by a [org.jetbrains.kotlin.backend.konan.BitcodePostProcessingContext], which closes it on dispose.
 */
internal class LlvmKotlinBridge(
        val config: TargetMachineConfig
) : AutoCloseable {

    companion object {
        /**
         * Loads the LLVM stubs library from [konanHome] and initializes the LLVM targets.
         * Thread-safe, and both steps happen at most once per process.
         */
        fun initialize(konanHome: String?) {
            loadLLVMStubs(konanHome)
            LLVMKotlinInitializeTargets()
        }
    }

    init {
        // The LLVM stubs are already loaded here: creating the config requires an LLVM module.
        LLVMKotlinInitializeTargets() // makes sure that the targets are initialized once
    }

    val targetMachine: LLVMTargetMachineRef = memScoped {
        val targetTriple = config.targetTriple
        val target = alloc<LLVMTargetRefVar>()
        val llvmError = alloc<CPointerVar<ByteVar>>()
        if (LLVMGetTargetFromTriple(targetTriple, target.ptr, llvmError.ptr) != 0) {
            val message = llvmError.value?.toKString()
            LLVMDisposeMessage(llvmError.value)
            error("Cannot get target from triple $targetTriple: $message")
        }
        LLVMCreateTargetMachine(
                target.value, targetTriple, config.cpuModel, config.cpuFeatures,
                config.optLevel, config.relocMode, config.codeModel
        ) ?: error("Cannot create target machine for $targetTriple (cpu=${config.cpuModel})")
    }

    override fun close() {
        LLVMDisposeTargetMachine(targetMachine)
    }
}
