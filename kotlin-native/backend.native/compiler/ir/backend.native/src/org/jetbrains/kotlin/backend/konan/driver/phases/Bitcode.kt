/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMDumpModule
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.changePhaserStateType
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.checkLlvmModuleExternalCalls
import org.jetbrains.kotlin.backend.konan.createLTOFinalPipelineConfig
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.insertAliasToEntryPoint
import org.jetbrains.kotlin.backend.konan.llvm.LlvmModuleCompilation
import org.jetbrains.kotlin.backend.konan.llvm.LlvmModuleCompilationOwner
import org.jetbrains.kotlin.backend.konan.llvm.coverage.runCoveragePass
import org.jetbrains.kotlin.backend.konan.llvm.verifyModule
import org.jetbrains.kotlin.backend.konan.optimizations.RemoveRedundantSafepointsPass
import org.jetbrains.kotlin.backend.konan.optimizations.removeMultipleThreadDataLoads

/**
 * Write in-memory LLVM module to filesystem as a bitcode.
 *
 * TODO: Use explicit input (LLVMModule) and output (File)
 *  after static driver removal.
 */
internal val WriteBitcodeFilePhase = createSimpleNamedCompilerPhase<PhaseContext, LLVMModuleRef, String>(
        "WriteBitcodeFile",
        "Write bitcode file",
        outputIfNotEnabled = { _, _, _, _ -> error("WriteBitcodeFile be disabled") }
) { context, llvmModule ->
    val output = context.tempFiles.nativeBinaryFileName
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    insertAliasToEntryPoint(context, llvmModule)
    LLVMWriteBitcodeToFile(llvmModule, output)
    output
}

private val CheckExternalCallsPhase = makeLlvmProcessingPhase<PhaseContext>(
        name = "CheckExternalCalls",
        description = "Check external calls")
{ _, input ->
    checkLlvmModuleExternalCalls(input)
}

private val RewriteExternalCallsCheckerGlobals = makeLlvmProcessingPhase<PhaseContext>(
        name = "RewriteExternalCallsCheckerGlobals",
        description = "Rewrite globals for external calls checker after optimizer run"
) { _, input ->
    addFunctionsListSymbolForChecker(input)
}

private val BitcodeOptimizationPhase = makeLlvmProcessingPhase<PhaseContext>(
        name = "BitcodeOptimization",
        description = "Optimize bitcode",
) { context, input ->
    val llvmPipelineConfig = createLTOFinalPipelineConfig(context, input.targetTriple, closedWorld = input.closedWorld)
    LlvmOptimizationPipeline(llvmPipelineConfig, input.module, context).use {
        it.run()
    }
}

private val CoveragePhase = makeLlvmProcessingPhase<PhaseContext>(
        name = "Coverage",
        description = "Produce coverage information",
        op = { context, input -> runCoveragePass(context, input) }
)

private val RemoveRedundantSafepointsPhase = makeLlvmProcessingPhase<PhaseContext>(
        name = "RemoveRedundantSafepoints",
        description = "Remove function prologue safepoints inlined to another function",
        op = { context, llvm ->
            RemoveRedundantSafepointsPass().runOnModule(
                    module = llvm.module,
                    isSafepointInliningAllowed = context.shouldInlineSafepoints()
            )
        }
)

private val OptimizeTLSDataLoadsPhase = makeLlvmProcessingPhase<PhaseContext>(
        name = "OptimizeTLSDataLoads",
        description = "Optimize multiple loads of thread data",
        op = { _, input -> removeMultipleThreadDataLoads(input) }
)

internal val CStubsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "CStubs",
        description = "C stubs compilation",
        op = { context, _ -> produceCStubs(context) }
)

internal val LinkBitcodeDependenciesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "LinkBitcodeDependencies",
        description = "Link bitcode dependencies",
        op = { context, _ -> linkBitcodeDependencies(context) }
)

internal val VerifyBitcodePhase = createSimpleNamedCompilerPhase<PhaseContext, LLVMModuleRef>(
        name = "VerifyBitcode",
        description = "Verify bitcode",
        op = { _, llvmModule -> verifyModule(llvmModule) }
)

internal val PrintBitcodePhase = createSimpleNamedCompilerPhase<PhaseContext, LLVMModuleRef>(
        name = "PrintBitcode",
        description = "Print bitcode",
        op = { _, llvmModule -> LLVMDumpModule(llvmModule) }
)

private fun <C: PhaseContext> makeLlvmProcessingPhase(
        name: String,
        description: String,
        op: (C, LlvmModuleCompilation) -> Unit
): SimpleNamedCompilerPhase<C, LlvmModuleCompilation, LlvmModuleCompilation> = createSimpleNamedCompilerPhase(
                name,
                description,
                postactions = setOf(::llvmIrDumpCallback),
                outputIfNotEnabled = { _, _, _, llvm -> llvm },
                    op = { context: C, llvm ->
                        op(context, llvm)
                        llvm
                    }
        )

internal fun <C : PhaseContext> PhaseEngine<C>.runBitcodePostProcessing(llvm: LlvmModuleCompilation, enableCoveragePhase: Boolean) {
    val checkExternalCalls = context.config.configuration.getBoolean(KonanConfigKeys.CHECK_EXTERNAL_CALLS)
    if (checkExternalCalls) {
        runPhase(CheckExternalCallsPhase, llvm)
    }
    runPhase(BitcodeOptimizationPhase, llvm)
    if (enableCoveragePhase) {
        runPhase(CoveragePhase, llvm)
    }
    if (context.config.memoryModel == MemoryModel.EXPERIMENTAL) {
        runPhase(RemoveRedundantSafepointsPhase, llvm)
    }
    if (context.config.optimizationsEnabled) {
        runPhase(OptimizeTLSDataLoadsPhase, llvm)
    }
    if (checkExternalCalls) {
        runPhase(RewriteExternalCallsCheckerGlobals, llvm)
    }
}