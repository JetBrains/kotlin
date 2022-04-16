package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.konan.target.*
import java.io.Closeable

private fun initializeLlvmGlobalPassRegistry() {
    val passRegistry = LLVMGetGlobalPassRegistry()

    LLVMInitializeCore(passRegistry)
    LLVMInitializeTransformUtils(passRegistry)
    LLVMInitializeScalarOpts(passRegistry)
    LLVMInitializeVectorization(passRegistry)
    LLVMInitializeInstCombine(passRegistry)
    LLVMInitializeIPO(passRegistry)
    LLVMInitializeInstrumentation(passRegistry)
    LLVMInitializeAnalysis(passRegistry)
    LLVMInitializeIPA(passRegistry)
    LLVMInitializeCodeGen(passRegistry)
    LLVMInitializeTarget(passRegistry)
    LLVMInitializeObjCARCOpts(passRegistry)
}

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
)

private fun getCpuModel(context: Context): String {
    val target = context.config.target
    val configurables: Configurables = context.config.platform.configurables
    return configurables.targetCpu ?: run {
        context.reportCompilationWarning("targetCpu for target $target was not set. Targeting `generic` cpu.")
        "generic"
    }
}

private fun getCpuFeatures(context: Context): String =
        context.config.platform.configurables.targetCpuFeatures ?: ""

private fun tryGetInlineThreshold(context: Context): Int? {
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
 * Creates [LlvmPipelineConfig] that is used for [RuntimeLinkageStrategy.LinkAndOptimize].
 * There is no DCE or internalization here because optimized module will be linked later.
 * Still, runtime is not intended to be debugged by user, and we can optimize it pretty aggressively
 * even in debug compilation.
 */
internal fun createLTOPipelineConfigForRuntime(context: Context): LlvmPipelineConfig {
    val configurables: Configurables = context.config.platform.configurables
    return LlvmPipelineConfig(
            context.llvm.targetTriple,
            getCpuModel(context),
            getCpuFeatures(context),
            LlvmOptimizationLevel.AGGRESSIVE,
            LlvmSizeLevel.NONE,
            LLVMCodeGenOptLevel.LLVMCodeGenLevelAggressive,
            configurables.currentRelocationMode(context).translateToLlvmRelocMode(),
            LLVMCodeModel.LLVMCodeModelDefault,
            globalDce = false,
            internalize = false,
            objCPasses = configurables is AppleConfigurables,
            makeDeclarationsHidden = false,
            inlineThreshold = tryGetInlineThreshold(context)
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
internal fun createLTOFinalPipelineConfig(context: Context): LlvmPipelineConfig {
    val target = context.config.target
    val configurables: Configurables = context.config.platform.configurables
    val cpuModel = getCpuModel(context)
    val cpuFeatures = getCpuFeatures(context)
    val optimizationLevel: LlvmOptimizationLevel = when {
        context.shouldOptimize() -> LlvmOptimizationLevel.AGGRESSIVE
        context.shouldContainDebugInfo() -> LlvmOptimizationLevel.NONE
        else -> LlvmOptimizationLevel.DEFAULT
    }
    val sizeLevel: LlvmSizeLevel = when {
        // We try to optimize code as much as possible on embedded targets.
        target is KonanTarget.ZEPHYR ||
                target == KonanTarget.WASM32 -> LlvmSizeLevel.AGGRESSIVE
        context.shouldOptimize() -> LlvmSizeLevel.NONE
        context.shouldContainDebugInfo() -> LlvmSizeLevel.NONE
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
    val internalize = context.llvmModuleSpecification.isFinal
    // Hidden visibility makes symbols internal when linking the binary.
    // When producing dynamic library, this enables stripping unused symbols from binary with -dead_strip flag,
    // similar to DCE enabled by internalize but later:
    //
    // Important for binary size, workarounds references to undefined symbols from interop libraries.
    val makeDeclarationsHidden = context.config.produce == CompilerOutputKind.STATIC_CACHE
    val objcPasses = configurables is AppleConfigurables

    // Null value means that LLVM should use default inliner params
    // for the provided optimization and size level.
    val inlineThreshold: Int? = when {
        context.shouldOptimize() -> tryGetInlineThreshold(context)
        context.shouldContainDebugInfo() -> null
        else -> null
    }

    return LlvmPipelineConfig(
            context.llvm.targetTriple,
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
    )
}

/**
 * Prepares and executes LLVM LTO pipeline on the given [llvmModule].
 *
 * Note: this class is intentionally uncoupled from [Context].
 * Please avoid depending on it.
 */
class LlvmOptimizationPipeline(
        private val config: LlvmPipelineConfig,
        private val llvmModule: LLVMModuleRef,
        private val logger: LoggingContext? = null
) : Closeable {
    private val arena = Arena()

    private val targetMachine: LLVMTargetMachineRef by lazy {
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

    private val modulePasses = LLVMCreatePassManager()
    private val passBuilder = LLVMPassManagerBuilderCreate()

    private fun init() {
        initLLVMTargets()
        initializeLlvmGlobalPassRegistry()
    }

    private fun populatePasses() {
        LLVMPassManagerBuilderSetOptLevel(passBuilder, config.optimizationLevel.value)
        LLVMPassManagerBuilderSetSizeLevel(passBuilder, config.sizeLevel.value)
        LLVMKotlinAddTargetLibraryInfoWrapperPass(modulePasses, config.targetTriple)
        // TargetTransformInfo pass.
        LLVMAddAnalysisPasses(targetMachine, modulePasses)
        if (config.internalize) {
            LLVMAddInternalizePass(modulePasses, 0)
        }
        if (config.makeDeclarationsHidden) {
            makeVisibilityHiddenLikeLlvmInternalizePass(llvmModule)
        }
        if (config.globalDce) {
            LLVMAddGlobalDCEPass(modulePasses)
        }
        config.inlineThreshold?.let { threshold ->
            LLVMPassManagerBuilderUseInlinerWithThreshold(passBuilder, threshold)
        }

        // Pipeline that is similar to `llvm-lto`.
        LLVMPassManagerBuilderPopulateLTOPassManager(passBuilder, modulePasses, Internalize = 0, RunInliner = 1)

        if (config.objCPasses) {
            // Lower ObjC ARC intrinsics (e.g. `@llvm.objc.clang.arc.use(...)`).
            // While Kotlin/Native codegen itself doesn't produce these intrinsics, they might come
            // from cinterop "glue" bitcode.
            // TODO: Consider adding other ObjC passes.
            LLVMAddObjCARCContractPass(modulePasses)
        }
    }

    fun run() {
        init()
        populatePasses()
        logger?.log {
            """
            Running LLVM optimizations with the following parameters:
            target_triple: ${config.targetTriple}
            cpu_model: ${config.cpuModel}
            cpu_features: ${config.cpuFeatures}
            optimization_level: ${config.optimizationLevel.value}
            size_level: ${config.sizeLevel.value}
            inline_threshold: ${config.inlineThreshold ?: "default"}
        """.trimIndent()
        }
        LLVMRunPassManager(modulePasses, llvmModule)
    }

    override fun close() {
        LLVMPassManagerBuilderDispose(passBuilder)
        LLVMDisposeTargetMachine(targetMachine)
        LLVMDisposePassManager(modulePasses)
        arena.clear()
    }

    companion object {
        @Volatile
        @JvmStatic
        private var isInitialized: Boolean = false

        @Synchronized
        @JvmStatic
        fun initLLVMTargets() {
            if (!isInitialized) {
                memScoped {
                    LLVMKotlinInitializeTargets()
                }
                isInitialized = true
            }
        }
    }
}

internal fun RelocationModeFlags.currentRelocationMode(context: Context): RelocationModeFlags.Mode =
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