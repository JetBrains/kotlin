/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMModuleCreateWithNameInContext
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.llvmLinkModules2

/**
 * To avoid combinatorial explosion, we split runtime into several LLVM modules.
 * This approach might cause performance degradation in some compilation modes because
 * there is no LTO between runtime modules.
 * RuntimeLinkageStrategy allows to choose the way we link runtime into final application or cache
 * and mitigate the problem above.
 *
 */
internal sealed class RuntimeLinkageStrategy {

    abstract fun run(): List<LLVMModuleRef>

    /**
     * Link runtime "as is", without any optimizations. Doable for "release" because LTO
     * in this mode is quite aggressive.
     */
    class Raw(private val runtimeNativeLibraries: List<LLVMModuleRef>) : RuntimeLinkageStrategy() {

        override fun run(): List<LLVMModuleRef> =
                runtimeNativeLibraries
    }

    /**
     * Links all runtime modules into a single one and optimizes it.
     */
    class LinkAndOptimize(
            private val generationState: NativeGenerationState,
            private val runtimeNativeLibraries: List<LLVMModuleRef>
    ) : RuntimeLinkageStrategy() {

        override fun run(): List<LLVMModuleRef> {
            if (runtimeNativeLibraries.isEmpty()) {
                return emptyList()
            }
            val runtimeModule = LLVMModuleCreateWithNameInContext("runtime", generationState.llvmContext)!!
            runtimeNativeLibraries.forEach {
                val failed = llvmLinkModules2(generationState, runtimeModule, it)
                if (failed != 0) {
                    throw Error("Failed to link ${it.getName()}")
                }
            }
            val config = createLTOPipelineConfigForRuntime(generationState)

            // TODO: reconsider pipeline here. Module optimizations instead of LTO can make a lot of sense, but require testing
            MandatoryOptimizationPipeline(config, generationState).use {
                it.execute(runtimeModule)
            }
            LTOOptimizationPipeline(config, generationState).use {
                it.execute(runtimeModule)
            }

            return listOf(runtimeModule)
        }
    }

    companion object {
        /**
         * Choose runtime linkage strategy based on current compiler configuration and [BinaryOptions.linkRuntime].
         */
        internal fun pick(generationState: NativeGenerationState, runtimeLlvmModules: List<LLVMModuleRef>): RuntimeLinkageStrategy {
            val config = generationState.config
            val binaryOption = config.configuration.get(BinaryOptions.linkRuntime)
            return when {
                binaryOption == RuntimeLinkageStrategyBinaryOption.Raw -> Raw(runtimeLlvmModules)
                binaryOption == RuntimeLinkageStrategyBinaryOption.Optimize -> LinkAndOptimize(generationState, runtimeLlvmModules)
                config.debug -> LinkAndOptimize(generationState, runtimeLlvmModules)
                else -> Raw(runtimeLlvmModules)
            }

        }
    }
}

enum class RuntimeLinkageStrategyBinaryOption {
    Raw,
    Optimize
}