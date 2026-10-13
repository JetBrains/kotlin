/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMDumpModule
import llvm.LLVMIsDeclaration
import llvm.LLVMModuleRef
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
import org.jetbrains.kotlin.backend.konan.llvm.LlvmBackendContext
import org.jetbrains.kotlin.backend.konan.llvm.LlvmFunctionAttribute
import org.jetbrains.kotlin.backend.konan.llvm.addLlvmFunctionEnumAttribute
import org.jetbrains.kotlin.backend.konan.llvm.getFunctions
import org.jetbrains.kotlin.backend.konan.llvm.valueName
import org.jetbrains.kotlin.backend.konan.llvm.verifyModule
import org.jetbrains.kotlin.backend.konan.optimizations.RemoveRedundantSafepointsPass
import org.jetbrains.kotlin.config.nativeBinaryOptions.SanitizerKind
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File
import kotlin.sequences.forEach

internal data class InsertEntryPointAliasInput(
        override val llvmModule: LLVMModuleRef,
        val entryPointName: String,
) : LlvmIrHolder

internal val InsertEntryPointAliasPhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, InsertEntryPointAliasInput>(
        "InsertEntryPointAlias"
) { _, (llvmModule, entryPointName) ->
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    insertAliasToEntryPoint(llvmModule, entryPointName)
}

internal val ModuleCallsChecker = optimizationPipelinePass(
        name = "ModuleCallsChecker",
        pipeline = ::ModuleCallsCheckerPipeline
)

internal val CallsChecker = optimizationPipelinePass(
        name = "CallsChecker",
        pipeline = ::CallsCheckerPipeline
)

internal class OptimizationState(
        config: NativeSecondStageCompilationConfig,
        val llvmConfig: LlvmPassesPipelineConfig,
        val llvmBackendContext: LlvmBackendContext,
        override val performanceManager: PerformanceManager?,
) : BasicNativeBackendPhaseContext(config)

internal fun optimizationPipelinePass(name: String, pipeline: (LlvmPassesPipelineConfig, LlvmBackendContext, PerformanceManager?, LoggingContext) -> LlvmOptimizationPipeline) =
        createSimpleNamedCompilerPhase<OptimizationState, LLVMModuleRef>(
                name = name,
                postactions = getDefaultLlvmModuleActions(),
        ) { context, module ->
            pipeline(
                    context.llvmConfig.copyConfiguringSaveIr(context, name),
                    context.llvmBackendContext,
                    context.performanceManager,
                    context
            ).execute(module)
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
                        .filter { LLVMIsDeclaration(it) == 0 && it.valueName != "__clang_call_terminate" }
                        .forEach { addLlvmFunctionEnumAttribute(it, sspAttribute) }
            }
        }
)

internal val StackProtectorPhaseInLLVM = optimizationPipelinePass(
        name = "StackProtectorPhase",
        pipeline = ::StackProtectorPipeline
)

internal val RemoveRedundantSafepointsPhaseInCompiler = createSimpleNamedCompilerPhase<BitcodePostProcessingContext, Unit>(
        name = "RemoveRedundantSafepoints",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ ->
            RemoveRedundantSafepointsPass().runOnModule(
                    module = context.llvm.module,
                    isSafepointInliningAllowed = context.shouldInlineSafepoints()
            )
        }
)

internal val RemoveRedundantSafepointsPhaseInLLVM = optimizationPipelinePass(
        name = "RemoveRedundantSafepoints",
        pipeline = ::RemoveRedundantSafepointsPipeline
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

/**
 * Runs LLVM postprocessing of [BitcodePostProcessingContext.llvmModule].
 * Please note that [llvmBackendContext] is borrowed: it is owned (and disposed) by the caller.
 */
internal fun <T : BitcodePostProcessingContext> PhaseEngine<T>.runBitcodePostProcessing(llvmBackendContext: LlvmBackendContext) {
    val optimizationConfig = createLTOFinalPipelineConfigFrom(
            context = context,
            closedWorld = context.config.isFinalBinary,
            timePasses = context.config.phaseConfig.needProfiling,
    )
    useContext(OptimizationState(context.config, optimizationConfig, llvmBackendContext, context.performanceManager)) {
        val module = this@runBitcodePostProcessing.context.llvmModule
        if (context.config.checkStateAtExternalCalls) {
            // Instrument functions before running any lowerings and optimizations, that could complicate the callgraph.
            it.runAndMeasurePhase(CallsChecker, module)
        }
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
        if (!context.config.runLLVMPassesInCompiler) {
            it.runAndMeasurePhase(RemoveRedundantSafepointsPhaseInLLVM, module)
        }
        if (context.config.checkStateAtExternalCalls) {
            // The list of known functions has to be created after all optimizations (notably, DCE).
            it.runAndMeasurePhase(ModuleCallsChecker, module)
        }
    }
    if (context.config.runLLVMPassesInCompiler) {
        runAndMeasurePhase(RemoveRedundantSafepointsPhaseInCompiler)
    }
}
