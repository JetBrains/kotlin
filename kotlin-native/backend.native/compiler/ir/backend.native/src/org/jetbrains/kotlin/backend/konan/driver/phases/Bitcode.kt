/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMDumpModule
import llvm.LLVMIsDeclaration
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
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
import org.jetbrains.kotlin.backend.konan.optimizations.removeMultipleThreadDataLoads
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
) { context, (llvmModule, outputFile) ->
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    insertAliasToEntryPoint(context, llvmModule)
    LLVMWriteBitcodeToFile(llvmModule, outputFile.canonicalPath)
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
            pipeline(context.llvmConfig, context.performanceManager, context).use {
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

internal val StackProtectorPhase = createSimpleNamedCompilerPhase<OptimizationState, LLVMModuleRef>(
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

internal val OptimizeTLSDataLoadsPhase = createSimpleNamedCompilerPhase<BitcodePostProcessingContext, Unit>(
        name = "OptimizeTLSDataLoads",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ -> removeMultipleThreadDataLoads(context) }
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
        it.runAndMeasurePhase(StackProtectorPhase, module)
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
    if (context.config.optimizationsEnabled) {
        runAndMeasurePhase(OptimizeTLSDataLoadsPhase)
    }
}
