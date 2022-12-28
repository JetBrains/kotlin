/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMDumpModule
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.common.phaser.Action
import org.jetbrains.kotlin.backend.common.phaser.ActionState
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.checkLlvmModuleExternalCalls
import org.jetbrains.kotlin.backend.konan.createLTOFinalPipelineConfig
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.insertAliasToEntryPoint
import org.jetbrains.kotlin.backend.konan.llvm.coverage.runCoveragePass
import org.jetbrains.kotlin.backend.konan.llvm.verifyModule
import org.jetbrains.kotlin.backend.konan.optimizations.RemoveRedundantSafepointsPass
import org.jetbrains.kotlin.backend.konan.optimizations.removeMultipleThreadDataLoads

private val nativeLLVMDumper =
        fun(actionState: ActionState, _: Unit, context: NativeGenerationState) {
            llvmIrDumpCallback(actionState, context.llvm.module, context.config, context.tempFiles)
        }

private val llvmPhaseActions: Set<Action<Unit, NativeGenerationState>> = setOf(nativeLLVMDumper)

/**
 * Write in-memory LLVM module to filesystem as a bitcode.
 *
 * TODO: Use explicit input (LLVMModule) and output (File)
 *  after static driver removal.
 */

internal val WriteBitcodeFilePhase = createSimpleNamedCompilerPhase<NativeGenerationState, LLVMModuleRef, String>(
        "WriteBitcodeFile",
        "Write bitcode file",
        outputIfNotEnabled = { _, _, _, _ -> error("WriteBitcodeFile be disabled") }
) { context, llvmModule ->
    val output = context.tempFiles.nativeBinaryFileName
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    insertAliasToEntryPoint(context)
    LLVMWriteBitcodeToFile(llvmModule, output)
    output
}

internal val CheckExternalCallsPhase = createSimpleNamedCompilerPhase(
        name = "CheckExternalCalls",
        description = "Check external calls",
        postactions = llvmPhaseActions,
) { context, _ ->
    checkLlvmModuleExternalCalls(context)
}

internal val RewriteExternalCallsCheckerGlobals = createSimpleNamedCompilerPhase(
        name = "RewriteExternalCallsCheckerGlobals",
        description = "Rewrite globals for external calls checker after optimizer run",
        postactions = llvmPhaseActions,
) { context, _ ->
    addFunctionsListSymbolForChecker(context)
}

internal val BitcodeOptimizationPhase = createSimpleNamedCompilerPhase(
        name = "BitcodeOptimization",
        description = "Optimize bitcode",
        postactions = llvmPhaseActions,
) { context, _ ->
    val config = createLTOFinalPipelineConfig(context, context.llvm.targetTriple, closedWorld = context.llvmModuleSpecification.isFinal)
    LlvmOptimizationPipeline(config, context.llvm.module, context).use {
        it.run()
    }
}

internal val CoveragePhase = createSimpleNamedCompilerPhase(
        name = "Coverage",
        description = "Produce coverage information",
        postactions = llvmPhaseActions,
        op = { context, _ -> runCoveragePass(context) }
)

internal val RemoveRedundantSafepointsPhase = createSimpleNamedCompilerPhase(
        name = "RemoveRedundantSafepoints",
        description = "Remove function prologue safepoints inlined to another function",
        postactions = llvmPhaseActions,
        op = { context, _ ->
            RemoveRedundantSafepointsPass().runOnModule(
                    module = context.llvm.module,
                    isSafepointInliningAllowed = context.shouldInlineSafepoints()
            )
        }
)

internal val OptimizeTLSDataLoadsPhase = createSimpleNamedCompilerPhase(
        name = "OptimizeTLSDataLoads",
        description = "Optimize multiple loads of thread data",
        postactions = llvmPhaseActions,
        op = { context, _ -> removeMultipleThreadDataLoads(context) }
)

internal val CStubsPhase = createSimpleNamedCompilerPhase(
        name = "CStubs",
        description = "C stubs compilation",
        postactions = llvmPhaseActions,
        op = { context, _ -> produceCStubs(context) }
)

internal val LinkBitcodeDependenciesPhase = createSimpleNamedCompilerPhase(
        name = "LinkBitcodeDependencies",
        description = "Link bitcode dependencies",
        postactions = llvmPhaseActions,
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

internal fun PhaseEngine<NativeGenerationState>.runBitcodePostProcessing() {
    val checkExternalCalls = context.config.configuration.getBoolean(KonanConfigKeys.CHECK_EXTERNAL_CALLS)
    if (checkExternalCalls) {
        runPhase(CheckExternalCallsPhase)
    }
    runPhase(BitcodeOptimizationPhase)
    runPhase(CoveragePhase)
    if (context.config.memoryModel == MemoryModel.EXPERIMENTAL) {
        runPhase(RemoveRedundantSafepointsPhase)
    }
    if (context.config.optimizationsEnabled) {
        runPhase(OptimizeTLSDataLoadsPhase)
    }
    if (checkExternalCalls) {
        runPhase(RewriteExternalCallsCheckerGlobals)
    }
}