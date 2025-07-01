/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.backend.common.reportCompilationWarning
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode
import org.jetbrains.kotlin.konan.target.*
import java.io.Closeable

enum class LlvmOptimizationLevel(val value: Int) {
    NONE(0),
    DEFAULT(1),
    AGGRESSIVE(3)
}

enum class LlvmSizeLevel(val value: Int) {
    NONE(0),
    DEFAULT(1),
    AGGRESSIVE(2)
}

/**
 * Incorporates everything that is used to tune a [LlvmOptimizationPipeline].
 */
data class LlvmPipelineConfig(
        val targetTriple: String,
        val cpuModel: String,
        val cpuFeatures: String,
        val optimizationLevel: LlvmOptimizationLevel,
        val sizeLevel: LlvmSizeLevel,
        val codegenOptimizationLevel: LLVMCodeGenOptLevel,
        val relocMode: LLVMRelocMode,
        val codeModel: LLVMCodeModel,
        val globalDce: Boolean,
        val internalize: Boolean,
        val makeDeclarationsHidden: Boolean,
        val objCPasses: Boolean,
        val inlineThreshold: Int?,
        val timePasses: Boolean = false,
        val modulePasses: String? = null,
        val ltoPasses: String? = null,
        val sspMode: StackProtectorMode = StackProtectorMode.NO,
)

private fun getCpuModel(context: PhaseContext): String {
    val target = context.config.target
    val configurables: Configurables = context.config.platform.configurables
    return configurables.targetCpu ?: run {
        context.reportCompilationWarning("targetCpu for target $target was not set. Targeting `generic` cpu.")
        "generic"
    }
}

private fun getCpuFeatures(context: PhaseContext): String =
        context.config.platform.configurables.targetCpuFeatures ?: ""

private fun tryGetInlineThreshold(context: PhaseContext): Int? {
    val configurables: Configurables = context.config.platform.configurables
    return configurables.llvmInlineThreshold?.let {
        it.toIntOrNull() ?: run {
            context.reportCompilationWarning(
                    "`llvmInlineThreshold` should be an integer. Got `$it` instead. Using default value."
            )
            null
        }
    }
}

/**
 * Creates [LlvmPipelineConfig] that is used for [org.jetbrains.kotlin.config.nativeBinaryOptions.RuntimeLinkageStrategy.LinkAndOptimize].
 * There is no DCE or internalization here because optimized module will be linked later.
 * Still, runtime is not intended to be debugged by user, and we can optimize it pretty aggressively
 * even in debug compilation.
 */
internal fun createLTOPipelineConfigForRuntime(generationState: NativeGenerationState): LlvmPipelineConfig {
    val config = generationState.config
    val configurables: Configurables = config.platform.configurables
    return LlvmPipelineConfig(
            generationState.llvm.targetTriple,
            getCpuModel(generationState),
            getCpuFeatures(generationState),
            LlvmOptimizationLevel.AGGRESSIVE,
            if (config.smallBinary) LlvmSizeLevel.AGGRESSIVE else LlvmSizeLevel.NONE,
            LLVMCodeGenOptLevel.LLVMCodeGenLevelAggressive,
            configurables.currentRelocationMode(generationState).translateToLlvmRelocMode(),
            LLVMCodeModel.LLVMCodeModelDefault,
            globalDce = false,
            internalize = false,
            objCPasses = configurables is AppleConfigurables,
            makeDeclarationsHidden = false,
            inlineThreshold = tryGetInlineThreshold(generationState),
            modulePasses = config.llvmModulePasses,
            ltoPasses = config.llvmLTOPasses,
            sspMode = config.stackProtectorMode,
    )
}

/**
 * In the end, Kotlin/Native generates a single LLVM module during compilation.
 * It won't be linked with any other LLVM module, so we can hide and DCE unused symbols.
 *
 * The set of optimizations relies on current compiler configuration.
 * In case of debug we do almost nothing (that's why we need [createLTOPipelineConfigForRuntime]),
 * but for release binaries we rely on "closed" world and enable a lot of optimizations.
 */
internal fun createLTOFinalPipelineConfig(
        context: PhaseContext,
        targetTriple: String,
        closedWorld: Boolean,
        timePasses: Boolean = false,
): LlvmPipelineConfig {
    val config = context.config
    val target = config.target
    val configurables: Configurables = config.platform.configurables
    val cpuModel = getCpuModel(context)
    val cpuFeatures = getCpuFeatures(context)
    val optimizationLevel: LlvmOptimizationLevel = when {
        context.shouldOptimize() -> LlvmOptimizationLevel.AGGRESSIVE
        context.shouldContainDebugInfo() -> LlvmOptimizationLevel.NONE
        else -> LlvmOptimizationLevel.DEFAULT
    }
    val sizeLevel: LlvmSizeLevel = when {
        // Only optimize for size, when required by configuration. LlvmSizeLevel.AGGRESSIVE will translate to "Oz"
        config.smallBinary -> LlvmSizeLevel.AGGRESSIVE
        else -> LlvmSizeLevel.NONE
    }
    val codegenOptimizationLevel: LLVMCodeGenOptLevel = when {
        context.shouldOptimize() -> LLVMCodeGenOptLevel.LLVMCodeGenLevelAggressive
        context.shouldContainDebugInfo() -> LLVMCodeGenOptLevel.LLVMCodeGenLevelNone
        else -> LLVMCodeGenOptLevel.LLVMCodeGenLevelDefault
    }
    val relocMode: LLVMRelocMode = configurables.currentRelocationMode(context).translateToLlvmRelocMode()
    val codeModel: LLVMCodeModel = LLVMCodeModel.LLVMCodeModelDefault
    val globalDce = true
    // Since we are in a "closed world" internalization can be safely used
    // to reduce size of a bitcode with global dce.
    val internalize = closedWorld
    // Hidden visibility makes symbols internal when linking the binary.
    // When producing dynamic library, this enables stripping unused symbols from binary with -dead_strip flag,
    // similar to DCE enabled by internalize but later:
    //
    // Important for binary size, workarounds references to undefined symbols from interop libraries.
    val makeDeclarationsHidden = config.produce == CompilerOutputKind.STATIC_CACHE
    val objcPasses = configurables is AppleConfigurables

    // Null value means that LLVM should use default inliner params
    // for the provided optimization and size level.
    val inlineThreshold: Int? = when {
        context.shouldOptimize() -> tryGetInlineThreshold(context)
        context.shouldContainDebugInfo() -> null
        else -> null
    }

    return LlvmPipelineConfig(
            targetTriple,
            cpuModel,
            cpuFeatures,
            optimizationLevel,
            sizeLevel,
            codegenOptimizationLevel,
            relocMode,
            codeModel,
            globalDce,
            internalize,
            makeDeclarationsHidden,
            objcPasses,
            inlineThreshold,
            timePasses = timePasses,
            modulePasses = config.llvmModulePasses,
            ltoPasses = config.llvmLTOPasses,
            sspMode = config.stackProtectorMode,
    )
}

abstract class LlvmOptimizationPipeline(
        private val config: LlvmPipelineConfig,
        private val logger: LoggingContext? = null
) : Closeable {
    open fun executeCustomPreprocessing(config: LlvmPipelineConfig, module: LLVMModuleRef) {}

    abstract val pipelineName: String
    abstract val passes: List<String>
    val optimizationFlag = when (config.sizeLevel) {
        LlvmSizeLevel.NONE -> "O${config.optimizationLevel.value}"
        LlvmSizeLevel.DEFAULT -> "Os"
        LlvmSizeLevel.AGGRESSIVE -> "Oz"
    }

    private val arena = Arena()
    private val targetMachineDelegate = lazy {
        val target = arena.alloc<LLVMTargetRefVar>()
        val foundLlvmTarget = LLVMGetTargetFromTriple(config.targetTriple, target.ptr, null) == 0
        check(foundLlvmTarget) { "Cannot get target from triple ${config.targetTriple}." }
        LLVMCreateTargetMachine(
                target.value,
                config.targetTriple,
                config.cpuModel,
                config.cpuFeatures,
                config.codegenOptimizationLevel,
                config.relocMode,
                config.codeModel)!!
    }

    private val targetMachine: LLVMTargetMachineRef by targetMachineDelegate

    fun execute(llvmModule: LLVMModuleRef) {
        val options: LLVMPassBuilderOptionsRef = LLVMCreatePassBuilderOptions()!!
        try {
            initLLVMOnce()
            config.inlineThreshold?.let { threshold ->
                if (threshold >= 0) {
                    LLVMPassBuilderOptionsSetInlinerThreshold(options, threshold)
                }
            }
            LLVMPassBuilderOptionsSetMaxDevirtIterations(options, 0)
            if (config.timePasses) {
                LLVMSetTimePasses(1)
            }
            executeCustomPreprocessing(config, llvmModule)
            val passDescription = passes.joinToString(",")
            logger?.log {
                """
                    Running ${pipelineName} with the following parameters:
                    target_triple: ${config.targetTriple}
                    cpu_model: ${config.cpuModel}
                    cpu_features: ${config.cpuFeatures}
                    optimization_level: ${config.optimizationLevel.value}
                    size_level: ${config.sizeLevel.value}
                    inline_threshold: ${config.inlineThreshold ?: "default"}
                    passes: ${passDescription}
                """.trimIndent()
            }
            if (passDescription.isEmpty()) return
            val errorCode = LLVMRunPasses(llvmModule, passDescription, targetMachine, options)
            require(errorCode == null) {
                LLVMGetErrorMessage(errorCode)!!.toKString()
            }
            if (config.timePasses) {
                LLVMPrintAllTimersToStdOut()
                LLVMClearAllTimers()
            }
        } finally {
            LLVMDisposePassBuilderOptions(options)
        }
    }

    override fun close() {
        if (targetMachineDelegate.isInitialized()) {
            LLVMDisposeTargetMachine(targetMachine)
        }
        arena.clear()
    }

    companion object {
        private var isInitialized: Boolean = false

        private fun initLLVMTargets() {
            memScoped {
                LLVMKotlinInitializeTargets()
            }
        }

        @Synchronized
        fun initLLVMOnce() {
            if (!isInitialized) {
                initLLVMTargets()
                isInitialized = true
            }
        }
    }
}

class MandatoryOptimizationPipeline(config: LlvmPipelineConfig, logger: LoggingContext? = null) :
        LlvmOptimizationPipeline(config, logger) {
    override val pipelineName = "New PM Mandatory llvm optimizations"
    override val passes = buildList {
        if (config.objCPasses) {
            // Lower ObjC ARC intrinsics (e.g. `@llvm.objc.clang.arc.use(...)`).
            // While Kotlin/Native codegen itself doesn't produce these intrinsics, they might come
            // from cinterop "glue" bitcode.
            // TODO: Consider adding other ObjC passes.
            add("objc-arc-contract")
        }

    }

    override fun executeCustomPreprocessing(config: LlvmPipelineConfig, module: LLVMModuleRef) {
        if (config.makeDeclarationsHidden) {
            makeVisibilityHiddenLikeLlvmInternalizePass(module)
        }
    }
}

class ModuleOptimizationPipeline(config: LlvmPipelineConfig, logger: LoggingContext? = null) :
        LlvmOptimizationPipeline(config, logger) {
    override val pipelineName = "New PM Module LLVM optimizations"
    override val passes = listOf(config.modulePasses ?: "default<$optimizationFlag>")
}

class LTOOptimizationPipeline(config: LlvmPipelineConfig, logger: LoggingContext? = null) :
        LlvmOptimizationPipeline(config, logger) {
    override val pipelineName = "New PM LTO LLVM optimizations"
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

class ThreadSanitizerPipeline(config: LlvmPipelineConfig, logger: LoggingContext? = null) :
        LlvmOptimizationPipeline(config, logger) {
    override val pipelineName = "New PM thread sanitizer"
    override val passes = listOf("tsan-module,function(tsan)")

    override fun executeCustomPreprocessing(config: LlvmPipelineConfig, module: LLVMModuleRef) {
        getFunctions(module)
                .filter { LLVMIsDeclaration(it) == 0 }
                .forEach { addLlvmFunctionEnumAttribute(it, LlvmFunctionAttribute.SanitizeThread) }
    }
}

internal fun RelocationModeFlags.currentRelocationMode(context: PhaseContext): RelocationModeFlags.Mode =
        when (determineLinkerOutput(context)) {
            LinkerOutputKind.DYNAMIC_LIBRARY -> dynamicLibraryRelocationMode
            LinkerOutputKind.STATIC_LIBRARY -> staticLibraryRelocationMode
            LinkerOutputKind.EXECUTABLE -> executableRelocationMode
        }

private fun RelocationModeFlags.Mode.translateToLlvmRelocMode() = when (this) {
    RelocationModeFlags.Mode.PIC -> LLVMRelocMode.LLVMRelocPIC
    RelocationModeFlags.Mode.STATIC -> LLVMRelocMode.LLVMRelocStatic
    RelocationModeFlags.Mode.DEFAULT -> LLVMRelocMode.LLVMRelocDefault
}
