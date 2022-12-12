/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.addFunctionsListSymbolForChecker
import org.jetbrains.kotlin.backend.konan.checkLlvmModuleExternalCalls
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.insertAliasToEntryPoint
import org.jetbrains.kotlin.backend.konan.llvm.coverage.runCoveragePass
import org.jetbrains.kotlin.backend.konan.optimizations.RemoveRedundantSafepointsPass
import org.jetbrains.kotlin.backend.konan.optimizations.removeMultipleThreadDataLoads

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
) { context, module: LLVMModuleRef ->
    val output = context.tempFiles.nativeBinaryFileName
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    insertAliasToEntryPoint(context)
    LLVMWriteBitcodeToFile(module, output)
    output
}

internal val CheckExternalCallsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "CheckExternalCalls",
        description = "Check external calls")
{ context, _ ->
    checkLlvmModuleExternalCalls(context)
}

internal val RewriteExternalCallsCheckerGlobals = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "RewriteExternalCallsCheckerGlobals",
        description = "Rewrite globals for external calls checker after optimizer run"
) { context, _ ->
    addFunctionsListSymbolForChecker(context)
}

internal val BitcodeOptimizationPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "BitcodeOptimization",
        description = "Optimize bitcode",
) { context, _ ->
    val config = createLTOFinalPipelineConfig(context)
    LlvmOptimizationPipeline(config, context.llvm.module, context).use {
        it.run()
    }
}

internal val CoveragePhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "Coverage",
        description = "Produce coverage information",
        op = { context, _ -> runCoveragePass(context) }
)

internal val RemoveRedundantSafepointsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "RemoveRedundantSafepoints",
        description = "Remove function prologue safepoints inlined to another function",
        op = { context, _ ->
            RemoveRedundantSafepointsPass(context).runOnModule(
                    module = context.llvm.module,
                    isSafepointInliningAllowed = context.shouldInlineSafepoints()
            )
        }
)

internal val OptimizeTLSDataLoadsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "OptimizeTLSDataLoads",
        description = "Optimize multiple loads of thread data",
        op = { context, _ -> removeMultipleThreadDataLoads(context) }
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

internal val VerifyBitcodePhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "VerifyBitcode",
        description = "Verify bitcode",
        op = { context, _ -> context.verifyBitCode() }
)

internal val PrintBitcodePhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "PrintBitcode",
        description = "Print bitcode",
        op = { context, _ ->
            if (context.shouldPrintBitCode()) {
                context.printBitCode()
            }
        }
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
