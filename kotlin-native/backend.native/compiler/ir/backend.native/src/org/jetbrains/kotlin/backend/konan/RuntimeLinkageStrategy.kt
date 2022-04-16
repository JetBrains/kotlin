/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMModuleCreateWithNameInContext
import llvm.LLVMModuleRef
import llvm.LLVMStripModuleDebugInfo
import org.jetbrains.kotlin.backend.konan.llvm.getName
import org.jetbrains.kotlin.backend.konan.llvm.llvmContext
import org.jetbrains.kotlin.backend.konan.llvm.llvmLinkModules2
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile

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
            private val context: Context,
            private val runtimeNativeLibraries: List<LLVMModuleRef>
    ) : RuntimeLinkageStrategy() {

        override fun run(): List<LLVMModuleRef> {
            if (runtimeNativeLibraries.isEmpty()) {
                return emptyList()
            }
            val runtimeModule = LLVMModuleCreateWithNameInContext("runtime", llvmContext)!!
            runtimeNativeLibraries.forEach {
                val failed = llvmLinkModules2(context, runtimeModule, it)
                if (failed != 0) {
                    throw Error("Failed to link ${it.getName()}")
                }
            }
            val config = createLTOPipelineConfigForRuntime(context)
            LlvmOptimizationPipeline(config, runtimeModule, context).use {
                it.run()
            }
            return listOf(runtimeModule)
        }
    }

    /**
     * Used in cases when runtime is not linked directly, e.g. it is a part of stdlib cache.
     */
    object None : RuntimeLinkageStrategy() {
        override fun run(): List<LLVMModuleRef> = emptyList()
    }

    companion object {
        /**
         * Choose runtime linkage strategy based on current compiler configuration and [BinaryOptions.linkRuntime].
         */
        internal fun pick(context: Context): RuntimeLinkageStrategy {
            val binaryOption = context.config.configuration.get(BinaryOptions.linkRuntime)
            val runtimeNativeLibraries = context.config.runtimeNativeLibraries
                    .takeIf { context.producedLlvmModuleContainsStdlib }
            val runtimeLlvmModules = runtimeNativeLibraries?.map {
                val parsedModule = parseBitcodeFile(it)
                if (!context.shouldUseDebugInfoFromNativeLibs()) {
                    LLVMStripModuleDebugInfo(parsedModule)
                }
                parsedModule
            }
            return when {
                runtimeLlvmModules == null -> return None
                binaryOption == RuntimeLinkageStrategyBinaryOption.Raw -> Raw(runtimeLlvmModules)
                binaryOption == RuntimeLinkageStrategyBinaryOption.Optimize -> LinkAndOptimize(context, runtimeLlvmModules)
                context.config.debug -> LinkAndOptimize(context, runtimeLlvmModules)
                else -> Raw(runtimeLlvmModules)
            }

        }
    }
}

enum class RuntimeLinkageStrategyBinaryOption {
    Raw,
    Optimize
}