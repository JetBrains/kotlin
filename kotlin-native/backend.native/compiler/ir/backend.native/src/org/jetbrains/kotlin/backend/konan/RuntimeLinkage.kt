/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMModuleCreateWithNameInContext
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.llvmLinkModules2
import org.jetbrains.kotlin.backend.konan.optimizations.handlePerformanceInlineAnnotation

/**
 * To avoid combinatorial explosion, we split runtime into several LLVM modules.
 * This approach might cause performance degradation in some compilation modes because there is no LTO between runtime modules.
 * RuntimeLinkageStrategy allows to choose the way we link runtime into final application or cache and mitigate the problem above.
 */
enum class RuntimeLinkageStrategy {
    /** Link runtime "as is", without any optimizations. Doable for "release" because LTO in this mode is quite aggressive. */
    Raw,

    /** Links all runtime modules into a single one and optimizes it. */
    Optimize
}

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
    val config = createLTOPipelineConfigForRuntime(generationState)

    MandatoryOptimizationPipeline(config, generationState).use {
        it.execute(runtimeModule)
    }
    ModuleOptimizationPipeline(config, generationState).use {
        it.execute(runtimeModule)
    }

    return listOf(runtimeModule)
}

/**
 * Perform runtime-specific bitcode processing (e.g. lower custom runtime annotations).
 */
private fun prepareRuntimeModule(generationState: NativeGenerationState, module: LLVMModuleRef) {
    handlePerformanceInlineAnnotation(generationState.config, BasicLlvmHelpers(generationState, module))
}
