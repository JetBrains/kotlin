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
import org.jetbrains.kotlin.konan.target.ZephyrConfigurables

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

    // Some of these values are copied from corresponding runtime.bc
    // which is using "generic" target CPU for many case.
    // This approach is suboptimal because target-cpu="generic" limits
    // the set of used cpu features.
    // TODO: refactor KonanTarget so that we can explicitly specify
    //  target cpu or arch+features combination in a single place.
    val cpuModel: String = when (target) {
        KonanTarget.IOS_ARM32 -> "generic"
        KonanTarget.IOS_ARM64 -> "cyclone"
        KonanTarget.IOS_X64 -> "core2"
        KonanTarget.TVOS_ARM64 -> "cyclone"
        KonanTarget.TVOS_X64 -> "core2"
        KonanTarget.WATCHOS_X86 -> "i386"
        KonanTarget.WATCHOS_X64 -> "core2"
        KonanTarget.WATCHOS_ARM64,
        KonanTarget.WATCHOS_ARM32 -> "cortex-a7"
        KonanTarget.LINUX_X64 -> "x86-64"
        KonanTarget.MINGW_X86 -> "pentium4"
        KonanTarget.MINGW_X64 -> "x86-64"
        KonanTarget.MACOS_X64 -> "core2"
        KonanTarget.LINUX_ARM32_HFP -> "arm1136jf-s"
        KonanTarget.LINUX_ARM64 -> "generic"
        KonanTarget.ANDROID_ARM32 -> "arm7tdmi"
        KonanTarget.ANDROID_ARM64 -> "cortex-a57"
        KonanTarget.ANDROID_X64 -> "x86-64"
        KonanTarget.ANDROID_X86 -> "i686"
        KonanTarget.LINUX_MIPS32 -> "mips32r2"
        KonanTarget.LINUX_MIPSEL32 -> "mips32r2"
        KonanTarget.WASM32 -> "generic"
        is KonanTarget.ZEPHYR -> (configurables as ZephyrConfigurables).targetCpu ?: run {
            context.reportCompilationWarning("targetCpu for target $target was not set. Targeting `generic` cpu.")
            "generic"
        }
    }

    val cpuFeatures: String = when (target) {
        KonanTarget.LINUX_ARM32_HFP -> "+dsp,+strict-align,+vfp2,-crypto,-d16,-fp-armv8,-fp-only-sp,-fp16,-neon,-thumb-mode,-vfp3,-vfp4"
        KonanTarget.ANDROID_ARM32 -> "+soft-float,+strict-align,-crypto,-neon,-thumb-mode"
        else -> ""
    }

    /**
     * Null value means that LLVM should use default inliner params
     * for the provided optimization and size level.
     */
    val customInlineThreshold: Int? = when {
        context.shouldOptimize() -> INLINE_THRESHOLD_OPT
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

    companion object {
        // By default LLVM uses 250 for -03 builds.
        // We use a smaller value since default value leads to
        // unreasonably bloated runtime code without any measurable
        // performance benefits.
        // This value still has to be tuned for different targets, though.
        private const val INLINE_THRESHOLD_OPT = 100
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