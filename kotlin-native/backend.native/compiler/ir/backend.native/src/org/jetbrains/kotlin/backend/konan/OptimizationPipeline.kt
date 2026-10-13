/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode
import org.jetbrains.kotlin.konan.config.saveLlvmIr
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.util.PerformanceManager

/**
 * Incorporates everything that is used to tune a [LlvmOptimizationPipeline].
 */
data class LlvmPassesPipelineConfig(
        val globalDce: Boolean,
        val internalize: Boolean,
        val makeDeclarationsHidden: Boolean,
        val objCPasses: Boolean,
        val inlineThreshold: Int?,
        val timePasses: Boolean = false,
        val modulePasses: String? = null,
        val ltoPasses: String? = null,
        val sspMode: StackProtectorMode = StackProtectorMode.NO,
        val saveIrAfterPasses: List<String> = emptyList(),
        val saveIrDirectory: java.io.File? = null,
        val runLLVMPassesInCompiler: Boolean,
        val shouldInlineSafepoints: Boolean = false,
) {
    /**
     * Create a copy of [LlvmPassesPipelineConfig] setting up options to dump IR
     */
    internal fun copyConfiguringSaveIr(context: NativeBackendPhaseContext, phase: String): LlvmPassesPipelineConfig {
        val passes = context.config.configuration.saveLlvmIr.mapNotNull {
            val llvmPassPrefix = "$phase:"
            if (it.startsWith(llvmPassPrefix)) {
                it.removePrefix(llvmPassPrefix)
            } else {
                null
            }
        }
        val saveIrDirectory = run {
            if (passes.isEmpty()) {
                null
            } else {
                val dir = context.config.saveLlvmIrDirectory
                if (dir.exists()) {
                    dir.resolve(phase) // This subdirectory does not have to exist: llvm will create it itself
                } else {
                    context.diagnosticReporter.report(
                            NativeBackendDiagnostics.LLVM_WARNING,
                            "Cannot dump LLVM IR to non-existent location: ${dir.absolutePath}")
                    null
                }
            }
        }
        val saveIrAfterPasses = if (saveIrDirectory == null) emptyList() else passes
        return copy(saveIrAfterPasses = saveIrAfterPasses, saveIrDirectory = saveIrDirectory)
    }
}

private fun tryGetInlineThreshold(context: NativeBackendPhaseContext): Int? {
    val configurables: Configurables = context.config.platform.configurables
    return configurables.llvmInlineThreshold?.let {
        it.toIntOrNull() ?: run {
            context.diagnosticReporter.report(NativeBackendDiagnostics.LLVM_WARNING,
                    "`llvmInlineThreshold` should be an integer. Got `$it` instead. Using default value."
            )
            null
        }
    }
}

/**
 * Creates [LlvmPassesPipelineConfig] that is used for [org.jetbrains.kotlin.config.nativeBinaryOptions.RuntimeLinkageStrategy.Optimize].
 * There is no DCE or internalization here because optimized module will be linked later.
 * Still, runtime is not intended to be debugged by user, and we can optimize it pretty aggressively
 * even in debug compilation.
 */
internal fun createLTOPipelineConfigForRuntimeFrom(context: NativeBackendPhaseContext): LlvmPassesPipelineConfig {
    val stageCompilationConfig = context.config
    val configurables = stageCompilationConfig.platform.configurables
    return LlvmPassesPipelineConfig(
            globalDce = false,
            internalize = false,
            objCPasses = configurables is AppleConfigurables,
            makeDeclarationsHidden = false,
            inlineThreshold = tryGetInlineThreshold(context),
            modulePasses = stageCompilationConfig.llvmModulePasses,
            ltoPasses = stageCompilationConfig.llvmLTOPasses,
            sspMode = stageCompilationConfig.stackProtectorMode,
            runLLVMPassesInCompiler = stageCompilationConfig.runLLVMPassesInCompiler,
    )
}

/**
 * In the end, Kotlin/Native generates a single LLVM module during compilation.
 * It won't be linked with any other LLVM module, so we can hide and DCE unused symbols.
 *
 * The set of optimizations relies on current compiler configuration.
 * In case of debug we do almost nothing (that's why we need [createLTOPipelineConfigForRuntimeFrom]),
 * but for release binaries we rely on "closed" world and enable a lot of optimizations.
 */
internal fun createLTOFinalPipelineConfigFrom(
        context: NativeBackendPhaseContext,
        closedWorld: Boolean,
        timePasses: Boolean = false,
): LlvmPassesPipelineConfig {
    val stageCompilationConfig = context.config
    val configurables = stageCompilationConfig.platform.configurables
    val globalDce = true
    // Since we are in a "closed world" internalization can be safely used
    // to reduce size of a bitcode with global dce.
    val internalize = closedWorld
    // Hidden visibility makes symbols internal when linking the binary.
    // When producing dynamic library, this enables stripping unused symbols from binary with -dead_strip flag,
    // similar to DCE enabled by internalize but later:
    //
    // Important for binary size, workarounds references to undefined symbols from interop libraries.
    val makeDeclarationsHidden = stageCompilationConfig.produce == CompilerOutputKind.STATIC_CACHE
    val objcPasses = configurables is AppleConfigurables
    // Null value means that LLVM should use default inliner params
    // for the provided optimization and size level.
    val inlineThreshold: Int? = when {
        context.shouldOptimize() -> tryGetInlineThreshold(context)
        context.shouldContainDebugInfo() -> null
        else -> null
    }
    return LlvmPassesPipelineConfig(
            globalDce,
            internalize,
            makeDeclarationsHidden,
            objcPasses,
            inlineThreshold,
            timePasses = timePasses,
            modulePasses = stageCompilationConfig.llvmModulePasses,
            ltoPasses = stageCompilationConfig.llvmLTOPasses,
            sspMode = stageCompilationConfig.stackProtectorMode,
            runLLVMPassesInCompiler = stageCompilationConfig.runLLVMPassesInCompiler,
            shouldInlineSafepoints = context.shouldInlineSafepoints(),
    )
}

abstract class LlvmOptimizationPipeline(
        private val config: LlvmPassesPipelineConfig,
        private val llvmBackendContext: LlvmBackendContext,
        private val performanceManager: PerformanceManager?,
        private val logger: LoggingContext? = null,
) {
    open fun executeCustomPreprocessing(config: LlvmPassesPipelineConfig, module: LLVMModuleRef) {}

    abstract val pipelineName: String
    abstract val passes: List<String>

    val optimizationFlag = when (llvmBackendContext.config.sizeLevel) {
        LlvmSizeLevel.NONE -> "O${llvmBackendContext.config.optimizationLevel.value}"
        LlvmSizeLevel.DEFAULT -> "Os"
        LlvmSizeLevel.AGGRESSIVE -> "Oz"
    }

    fun execute(llvmModule: LLVMModuleRef) {
        executeCustomPreprocessing(config, llvmModule)
        val passDescription = passes.joinToString(",")
        logger?.log {
            """
                Running $pipelineName with the following parameters:
                target_triple: ${llvmBackendContext.config.targetSpec.targetTriple}
                cpu_model: ${llvmBackendContext.config.targetSpec.cpuModel}
                cpu_features: ${llvmBackendContext.config.targetSpec.cpuFeatures}
                optimization_level: ${llvmBackendContext.config.optimizationLevel.value}
                size_level: ${llvmBackendContext.config.sizeLevel.value}
                inline_threshold: ${config.inlineThreshold ?: "default"}
                passes: $passDescription
            """.trimIndent()
        }
        if (passDescription.isEmpty()) return
        val [errorCode, profile] = withLLVMPassesProfile(performanceManager != null || config.timePasses, pipelineName) {
            // NOTE: This call is not thread-safe in general, because it may write into global memory,
            //       when configuring CLI-defined options.
            //       See `LLVMKotlinRunPasses` declaration in the header file for the details.
            LLVMKotlinRunPasses(
                    llvmModule,
                    passDescription,
                    llvmBackendContext.targetMachine,
                    InlinerThreshold = config.inlineThreshold ?: -1,
                    Profile = it,
                    SaveIRAfterPasses = config.saveIrAfterPasses.joinToString(",").takeIf { it.isNotEmpty() },
                    SaveIRDirectory = config.saveIrDirectory?.absolutePath,
            )
        }
        require(errorCode == null) {
            LLVMGetErrorMessage(errorCode)!!.toKString()
        }
        profile?.let {
            performanceManager?.addLlvmPassesProfile(it)
            if (config.timePasses) {
                it.print()
            }
        }
    }
}

class MandatoryOptimizationPipeline(
        config: LlvmPassesPipelineConfig,
        llvmBackendContext: LlvmBackendContext,
        performanceManager: PerformanceManager?,
        logger: LoggingContext? = null
) : LlvmOptimizationPipeline(config, llvmBackendContext, performanceManager, logger) {
    override val pipelineName = "llvm-mandatory"

    override val passes = buildList {
        if (!config.runLLVMPassesInCompiler && config.makeDeclarationsHidden) {
            add("kotlin-hide-symbols")
        }
        if (config.objCPasses) {
            // Lower ObjC ARC intrinsics (e.g. `@llvm.objc.clang.arc.use(...)`).
            // While Kotlin/Native codegen itself doesn't produce these intrinsics, they might come
            // from cinterop "glue" bitcode.
            // TODO: Consider adding other ObjC passes.
            add("objc-arc-contract")
        }
    }

    override fun executeCustomPreprocessing(config: LlvmPassesPipelineConfig, module: LLVMModuleRef) {
        if (config.runLLVMPassesInCompiler && config.makeDeclarationsHidden) {
            makeVisibilityHiddenLikeLlvmInternalizePass(module)
        }
    }
}

class ModuleOptimizationPipeline(
        config: LlvmPassesPipelineConfig,
        llvmBackendContext: LlvmBackendContext,
        performanceManager: PerformanceManager?,
        logger: LoggingContext? = null
) : LlvmOptimizationPipeline(config, llvmBackendContext, performanceManager, logger) {
    override val pipelineName = "llvm-default"
    override val passes = listOf(config.modulePasses ?: "default<$optimizationFlag>")
}

class LTOOptimizationPipeline(
        config: LlvmPassesPipelineConfig,
        llvmBackendContext: LlvmBackendContext,
        performanceManager: PerformanceManager?,
        logger: LoggingContext? = null
) : LlvmOptimizationPipeline(config, llvmBackendContext, performanceManager, logger) {
    override val pipelineName = "llvm-lto"
    override val passes =
            if (config.ltoPasses != null) listOf(config.ltoPasses)
            else buildList {
                if (config.internalize) {
                    add("internalize")
                }

                if (config.globalDce) {
                    add("globaldce")
                }

                // Pipeline that is similar to `llvm-lto`.
                add("lto<$optimizationFlag>")
            }
}

class ThreadSanitizerPipeline(
        config: LlvmPassesPipelineConfig,
        llvmBackendContext: LlvmBackendContext,
        performanceManager: PerformanceManager?,
        logger: LoggingContext? = null
) : LlvmOptimizationPipeline(config, llvmBackendContext, performanceManager, logger) {
    override val pipelineName = "llvm-tsan"
    override val passes = buildList {
        if (!config.runLLVMPassesInCompiler) {
            add("function(kotlin-tsan)")
        }
        add("tsan-module")
        add("function(tsan)")
    }

    override fun executeCustomPreprocessing(config: LlvmPassesPipelineConfig, module: LLVMModuleRef) {
        if (!config.runLLVMPassesInCompiler)
            return
        getFunctions(module)
                .filter { LLVMIsDeclaration(it) == 0 }
                .forEach { addLlvmFunctionEnumAttribute(it, LlvmFunctionAttribute.SanitizeThread) }
    }
}

class StackProtectorPipeline(
        config: LlvmPassesPipelineConfig,
        llvmBackendContext: LlvmBackendContext,
        performanceManager: PerformanceManager?, logger: LoggingContext? = null
) : LlvmOptimizationPipeline(config, llvmBackendContext, performanceManager, logger) {
    override val pipelineName = "llvm-ssp"
    override val passes = buildList {
        val arg = when (config.sspMode) {
            StackProtectorMode.NO -> null
            StackProtectorMode.YES -> ""
            StackProtectorMode.STRONG -> "<strong>"
            StackProtectorMode.ALL -> "<req>"
        }
        arg?.let {
            add("function(kotlin-ssp$it)")
        }
    }
}

class RemoveRedundantSafepointsPipeline(
        config: LlvmPassesPipelineConfig,
        llvmBackendContext: LlvmBackendContext,
        performanceManager: PerformanceManager?,
        logger: LoggingContext? = null
) : LlvmOptimizationPipeline(config, llvmBackendContext, performanceManager, logger) {
    override val pipelineName = "llvm-remove-sp"
    override val passes = buildList {
        val arg = if (config.shouldInlineSafepoints) "<inline>" else ""
        add("function(kotlin-remove-sp$arg)")
    }
}

class ModuleCallsCheckerPipeline(
        config: LlvmPassesPipelineConfig,
        llvmBackendContext: LlvmBackendContext,
        performanceManager: PerformanceManager?,
        logger: LoggingContext? = null
) : LlvmOptimizationPipeline(config, llvmBackendContext, performanceManager, logger) {
    override val pipelineName = "llvm-calls-checker-module"
    override val passes = listOf("kotlin-calls-checker-module")
}

class CallsCheckerPipeline(
        config: LlvmPassesPipelineConfig,
        llvmBackendContext: LlvmBackendContext,
        performanceManager: PerformanceManager?,
        logger: LoggingContext? = null
) : LlvmOptimizationPipeline(config, llvmBackendContext, performanceManager, logger) {
    override val pipelineName = "llvm-calls-checker"
    override val passes = listOf("function(kotlin-calls-checker)")
}
