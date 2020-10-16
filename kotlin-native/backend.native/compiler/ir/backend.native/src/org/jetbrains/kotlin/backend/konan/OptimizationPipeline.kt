package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.makeVisibilityHiddenLikeLlvmInternalizePass
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Configurables
import org.jetbrains.kotlin.konan.target.KonanTarget

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
}

internal fun shouldRunLateBitcodePasses(context: Context): Boolean {
    return context.coverage.enabled
}

internal fun runLateBitcodePasses(context: Context, llvmModule: LLVMModuleRef) {
    val passManager = LLVMCreatePassManager()!!
    LLVMKotlinAddTargetLibraryInfoWrapperPass(passManager, context.llvm.targetTriple)
    context.coverage.addLateLlvmPasses(passManager)
    LLVMRunPassManager(passManager, llvmModule)
    LLVMDisposePassManager(passManager)
}

private class LlvmPipelineConfiguration(context: Context) {

    private val target = context.config.target
    private val configurables: Configurables = context.config.platform.configurables

    val targetTriple: String = context.llvm.targetTriple

    val cpuModel: String = configurables.targetCpu ?: run {
        context.reportCompilationWarning("targetCpu for target $target was not set. Targeting `generic` cpu.")
        "generic"
    }

    val cpuFeatures: String = configurables.targetCpuFeatures ?: ""

    /**
     * Null value means that LLVM should use default inliner params
     * for the provided optimization and size level.
     */
    val customInlineThreshold: Int? = when {
        context.shouldOptimize() -> configurables.llvmInlineThreshold?.let {
            it.toIntOrNull() ?: run {
                context.reportCompilationWarning(
                        "`llvmInlineThreshold` should be an integer. Got `$it` instead. Using default value."
                )
                null
            }
        }
        context.shouldContainDebugInfo() -> null
        else -> null
    }

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

    val relocMode: LLVMRelocMode = LLVMRelocMode.LLVMRelocDefault

    val codeModel: LLVMCodeModel = LLVMCodeModel.LLVMCodeModelDefault

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
}

internal fun runLlvmOptimizationPipeline(context: Context) {
    val llvmModule = context.llvmModule!!
    val config = LlvmPipelineConfiguration(context)

    memScoped {
        LLVMKotlinInitializeTargets()

        initializeLlvmGlobalPassRegistry()
        val passBuilder = LLVMPassManagerBuilderCreate()
        val modulePasses = LLVMCreatePassManager()
        LLVMPassManagerBuilderSetOptLevel(passBuilder, config.optimizationLevel.value)
        LLVMPassManagerBuilderSetSizeLevel(passBuilder, config.sizeLevel.value)
        // TODO: use LLVMGetTargetFromName instead.
        val target = alloc<LLVMTargetRefVar>()
        val foundLlvmTarget = LLVMGetTargetFromTriple(config.targetTriple, target.ptr, null) == 0
        check(foundLlvmTarget) { "Cannot get target from triple ${config.targetTriple}." }

        val targetMachine = LLVMCreateTargetMachine(
                target.value,
                config.targetTriple,
                config.cpuModel,
                config.cpuFeatures,
                config.codegenOptimizationLevel,
                config.relocMode,
                config.codeModel)

        LLVMKotlinAddTargetLibraryInfoWrapperPass(modulePasses, config.targetTriple)
        // TargetTransformInfo pass.
        LLVMAddAnalysisPasses(targetMachine, modulePasses)
        if (context.llvmModuleSpecification.isFinal) {
            // Since we are in a "closed world" internalization can be safely used
            // to reduce size of a bitcode with global dce.
            LLVMAddInternalizePass(modulePasses, 0)
        } else if (context.config.produce == CompilerOutputKind.STATIC_CACHE) {
            // Hidden visibility makes symbols internal when linking the binary.
            // When producing dynamic library, this enables stripping unused symbols from binary with -dead_strip flag,
            // similar to DCE enabled by internalize but later:
            makeVisibilityHiddenLikeLlvmInternalizePass(llvmModule)
            // Important for binary size, workarounds references to undefined symbols from interop libraries.
        }
        LLVMAddGlobalDCEPass(modulePasses)

        config.customInlineThreshold?.let { threshold ->
            LLVMPassManagerBuilderUseInlinerWithThreshold(passBuilder, threshold)
        }
        // Pipeline that is similar to `llvm-lto`.
        // TODO: Add ObjC optimization passes.
        LLVMPassManagerBuilderPopulateLTOPassManager(passBuilder, modulePasses, Internalize = 0, RunInliner = 1)

        LLVMRunPassManager(modulePasses, llvmModule)

        LLVMPassManagerBuilderDispose(passBuilder)
        LLVMDisposeTargetMachine(targetMachine)
        LLVMDisposePassManager(modulePasses)
    }
    if (shouldRunLateBitcodePasses(context)) {
        runLateBitcodePasses(context, llvmModule)
    }
}