/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMDumpModule
import llvm.LLVMIsDeclaration
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode.ALL
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode.NO
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode.STRONG
import org.jetbrains.kotlin.config.nativeBinaryOptions.StackProtectorMode.YES
import org.jetbrains.kotlin.backend.konan.driver.BasicNativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.LlvmIrHolder
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultLlvmModuleActions
import org.jetbrains.kotlin.backend.konan.llvm.LlvmFunctionAttribute
import org.jetbrains.kotlin.backend.konan.llvm.addLlvmFunctionEnumAttribute
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import org.jetbrains.kotlin.backend.konan.llvm.name
import org.jetbrains.kotlin.backend.konan.llvm.verifyModule
import org.jetbrains.kotlin.backend.konan.optimizations.RemoveRedundantSafepointsPass
import org.jetbrains.kotlin.config.nativeBinaryOptions.SanitizerKind
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File
import kotlin.sequences.forEach


internal data class WriteBitcodeFileInput(
        override val llvmModule: LLVMModuleRef,
        val outputFile: File,
) : LlvmIrHolder

/**
 * Write in-memory LLVM module to filesystem as a bitcode.
 */
internal val WriteBitcodeFilePhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, WriteBitcodeFileInput>(
        "WriteBitcodeFile",
        postactions = getDefaultLlvmModuleActions(),
) { context, (llvmModule, outputFile) ->
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    insertAliasToEntryPoint(context, llvmModule)
    LLVMWriteBitcodeToFile(llvmModule, outputFile.canonicalPath)
}

internal data class CompileModuleToPtxInput(
        val bitcodeFile: File,
        val outputFile: File,
)

// sm_50 (Maxwell) is the lowest virtual architecture that supports the NVVM intrinsics
// used by `kotlin.native.cuda` and still runs on essentially every CUDA-capable card from
// the last decade. Future configuration could pull this from a per-binary option.
private const val NVPTX_DEFAULT_CPU = "sm_50"

/**
 * Emit a `.ptx` text artifact for a `Runtime.Kind.CudaDevice` fragment by shelling out to
 * the K/N-bundled `llc`. The bundled `llc` (`<llvmHome>/bin/llc`) has full NVPTX support,
 * including the `nvptx64` target and the NVPTX asm printer; the LLVM-side libraries are
 * present in the `dev-90` variant and `llc --version` lists `nvptx`/`nvptx64` under its
 * registered targets.
 *
 * Why a subprocess and not an in-process `LLVMTargetMachineEmitToFile`: directly linking
 * NVPTX libs into `libllvmstubs.dylib` and calling `LLVMInitializeNVPTXTarget*` in
 * `LLVMKotlinInitializeTargets` triggers a kernel SIGKILL of the JVM with no diagnostic
 * output on macOS arm64 (suspected W^X / code-signing interaction with NVPTX's
 * codegen-time JIT setup). The subprocess path side-steps the in-process integration
 * entirely while producing the same PTX output. When the in-process issue is diagnosed and
 * fixed, this phase can be swapped back to `LLVMTargetMachineEmitToFile` without touching
 * the surrounding dispatch.
 */
internal val CompileModuleToPtxPhase = createSimpleNamedCompilerPhase<NativeGenerationState, CompileModuleToPtxInput>(
        "CompileModuleToPtx",
) { context, input ->
    val llcPath = "${context.config.platform.absoluteLlvmHome}/bin/llc"
    Command(
            llcPath,
            "-mtriple=nvptx64-nvidia-cuda",
            "-mcpu=$NVPTX_DEFAULT_CPU",
            "-filetype=asm",
            input.bitcodeFile.absolutePath,
            "-o", input.outputFile.absolutePath,
    ).logWith(context::log).execute()
}

internal val CheckExternalCallsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "CheckExternalCalls",
        postactions = getDefaultLlvmModuleActions(),
) { context, _ ->
    checkLlvmModuleExternalCalls(context)
}

/**
 * Rewrites globals for external calls checker after optimizer run.
 */
internal val RewriteExternalCallsCheckerGlobals = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "RewriteExternalCallsCheckerGlobals",
        postactions = getDefaultLlvmModuleActions(),
) { context, _ ->
    addFunctionsListSymbolForChecker(context)
}

internal class OptimizationState(
        config: NativeSecondStageCompilationConfig,
        val llvmConfig: LlvmPipelineConfig,
        override val performanceManager: PerformanceManager?,
) : BasicNativeBackendPhaseContext(config)

internal fun optimizationPipelinePass(name: String, pipeline: (LlvmPipelineConfig, PerformanceManager?, LoggingContext) -> LlvmOptimizationPipeline) =
        createSimpleNamedCompilerPhase<OptimizationState, LLVMModuleRef>(
                name = name,
                postactions = getDefaultLlvmModuleActions(),
        ) { context, module ->
            pipeline(context.llvmConfig.copyConfiguringSaveIr(context, name), context.performanceManager, context).use {
                it.execute(module)
            }
        }

internal val MandatoryBitcodeLLVMPostprocessingPhase = optimizationPipelinePass(
        name = "MandatoryBitcodeLLVMPostprocessingPhase",
        pipeline = ::MandatoryOptimizationPipeline,
)

internal val ModuleBitcodeOptimizationPhase = optimizationPipelinePass(
        name = "ModuleBitcodeOptimization",
        pipeline = ::ModuleOptimizationPipeline,
)

internal val LTOBitcodeOptimizationPhase = optimizationPipelinePass(
        name = "LTOBitcodeOptimization",
        pipeline = ::LTOOptimizationPipeline
)

internal val ThreadSanitizerPhase = optimizationPipelinePass(
        name = "ThreadSanitizerPhase",
        pipeline = ::ThreadSanitizerPipeline
)

internal val StackProtectorPhaseInCompiler = createSimpleNamedCompilerPhase<OptimizationState, LLVMModuleRef>(
        name = "StackProtectorPhase",
        postactions = getDefaultLlvmModuleActions(),
        op = { context: OptimizationState, module: LLVMModuleRef ->
            val attribute = when (context.llvmConfig.sspMode) {
                NO -> null
                YES -> LlvmFunctionAttribute.Ssp
                STRONG -> LlvmFunctionAttribute.SspStrong
                ALL -> LlvmFunctionAttribute.SspReq
            }
            attribute?.let { sspAttribute ->
                getFunctions(module)
                        .filter { LLVMIsDeclaration(it) == 0 && it.name != "__clang_call_terminate" }
                        .forEach { addLlvmFunctionEnumAttribute(it, sspAttribute) }
            }
        }
)

internal val StackProtectorPhaseInLLVM = optimizationPipelinePass(
        name = "StackProtectorPhase",
        pipeline = ::StackProtectorPipeline
)

internal val RemoveRedundantSafepointsPhase = createSimpleNamedCompilerPhase<BitcodePostProcessingContext, Unit>(
        name = "RemoveRedundantSafepoints",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ ->
            RemoveRedundantSafepointsPass().runOnModule(
                    module = context.llvm.module,
                    isSafepointInliningAllowed = context.shouldInlineSafepoints()
            )
        }
)

internal val CStubsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "CStubs",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ -> produceCStubs(context) }
)

internal val LinkBitcodeDependenciesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, List<File>>(
        name = "LinkBitcodeDependencies",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, input -> linkBitcodeDependencies(context, input) }
)

internal val VerifyBitcodePhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, LLVMModuleRef>(
        name = "VerifyBitcode",
        op = { _, llvmModule -> verifyModule(llvmModule) }
)

internal val PrintBitcodePhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, LLVMModuleRef>(
        name = "PrintBitcode",
        op = { _, llvmModule -> LLVMDumpModule(llvmModule) }
)

internal fun <T : BitcodePostProcessingContext> PhaseEngine<T>.runBitcodePostProcessing() {
    val optimizationConfig = createLTOFinalPipelineConfig(
            context,
            context.llvm.targetTriple,
            closedWorld = context.config.isFinalBinary,
            timePasses = context.config.phaseConfig.needProfiling,
    )
    useContext(OptimizationState(context.config, optimizationConfig, context.performanceManager)) {
        val module = this@runBitcodePostProcessing.context.llvmModule
        if (context.config.runLLVMPassesInCompiler) {
            it.runAndMeasurePhase(StackProtectorPhaseInCompiler, module)
        } else {
            it.runAndMeasurePhase(StackProtectorPhaseInLLVM, module)
        }
        it.runAndMeasurePhase(MandatoryBitcodeLLVMPostprocessingPhase, module)
        it.runAndMeasurePhase(ModuleBitcodeOptimizationPhase, module)
        it.runAndMeasurePhase(LTOBitcodeOptimizationPhase, module)
        when (context.config.sanitizer) {
            SanitizerKind.THREAD -> it.runAndMeasurePhase(ThreadSanitizerPhase, module)
            SanitizerKind.ADDRESS -> context.reportCompilationError("Address sanitizer is not supported yet")
            null -> {}
        }
    }
    runAndMeasurePhase(RemoveRedundantSafepointsPhase)
}
