/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.runtime

import llvm.LLVMModuleCreateWithNameInContext
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.MandatoryOptimizationPipeline
import org.jetbrains.kotlin.backend.konan.ModuleOptimizationPipeline
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.config.nativeBinaryOptions.RuntimeLinkageStrategy
import org.jetbrains.kotlin.backend.konan.createLTOPipelineConfigForRuntimeFrom
import org.jetbrains.kotlin.backend.konan.llvm.BasicLlvmHelpers
import org.jetbrains.kotlin.backend.konan.llvm.createLlvmBackendConfigForRuntimeFrom
import org.jetbrains.kotlin.backend.konan.llvm.getName
import org.jetbrains.kotlin.backend.konan.llvm.llvmLinkModules2
import org.jetbrains.kotlin.backend.konan.llvm.withLlvmBackendContext
import org.jetbrains.kotlin.backend.konan.optimizations.handlePerformanceInlineAnnotation
import kotlin.collections.forEach

internal fun linkRuntimeModules(generationState: NativeGenerationState, runtimeNativeLibraries: List<LLVMModuleRef>): List<LLVMModuleRef> {
    if (runtimeNativeLibraries.isEmpty()) {
        return emptyList()
    }

    runtimeNativeLibraries.forEach {
        prepareRuntimeModule(generationState, it)
    }

    if (generationState.config.runtimeLinkageStrategy == RuntimeLinkageStrategy.Raw) {
        return runtimeNativeLibraries
    }

    val runtimeModule = LLVMModuleCreateWithNameInContext("runtime", generationState.llvmContext)!!
    runtimeNativeLibraries.forEach {
        val failed = llvmLinkModules2(generationState, runtimeModule, it)
        if (failed != 0) {
            throw Error("Failed to link ${it.getName()}")
        }
    }

    withLlvmBackendContext(createLlvmBackendConfigForRuntimeFrom(generationState)) {
        val pipelineConfig = createLTOPipelineConfigForRuntimeFrom(generationState)
        val llvmBackendContext = this
        MandatoryOptimizationPipeline(pipelineConfig, llvmBackendContext, generationState.performanceManager, generationState)
                .execute(runtimeModule)
        ModuleOptimizationPipeline(pipelineConfig, llvmBackendContext, generationState.performanceManager, generationState)
                .execute(runtimeModule)
    }

    return listOf(runtimeModule)
}

/**
 * Perform runtime-specific bitcode processing (e.g. lower custom runtime annotations).
 */
private fun prepareRuntimeModule(generationState: NativeGenerationState, module: LLVMModuleRef) {
    handlePerformanceInlineAnnotation(generationState.config, BasicLlvmHelpers(generationState, module))
}
