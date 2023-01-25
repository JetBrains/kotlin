/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMDumpModule
import llvm.LLVMModuleRef
import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.common.phaser.ActionState
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.checkLlvmModuleExternalCalls
import org.jetbrains.kotlin.backend.konan.createLTOFinalPipelineConfig
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.driver.utilities.LlvmIrHolder
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultLlvmModuleActions
import org.jetbrains.kotlin.backend.konan.insertAliasToEntryPoint
import org.jetbrains.kotlin.backend.konan.llvm.coverage.runCoveragePass
import org.jetbrains.kotlin.backend.konan.llvm.verifyModule
import org.jetbrains.kotlin.backend.konan.optimizations.RemoveRedundantSafepointsPass
import org.jetbrains.kotlin.backend.konan.optimizations.removeMultipleThreadDataLoads
import java.io.File


internal data class WriteBitcodeFileInput(
        override val llvmModule: LLVMModuleRef,
        val outputFile: File,
) : LlvmIrHolder

/**
 * Write in-memory LLVM module to filesystem as a bitcode.
 */
internal val WriteBitcodeFilePhase = createSimpleNamedCompilerPhase<PhaseContext, WriteBitcodeFileInput>(
        "WriteBitcodeFile",
        "Write bitcode file",
) { context, (llvmModule, outputFile) ->
    // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
    insertAliasToEntryPoint(context, llvmModule)
    LLVMWriteBitcodeToFile(llvmModule, outputFile.canonicalPath)
}

internal val CheckExternalCallsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "CheckExternalCalls",
        description = "Check external calls",
        postactions = getDefaultLlvmModuleActions(),
) { context, _ ->
    checkLlvmModuleExternalCalls(context)
}

internal val RewriteExternalCallsCheckerGlobals = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "RewriteExternalCallsCheckerGlobals",
        description = "Rewrite globals for external calls checker after optimizer run",
        postactions = getDefaultLlvmModuleActions(),
) { context, _ ->
    addFunctionsListSymbolForChecker(context)
}

internal val BitcodeOptimizationPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "BitcodeOptimization",
        description = "Optimize bitcode",
        postactions = getDefaultLlvmModuleActions(),
) { context, _ ->
    val config = createLTOFinalPipelineConfig(context, context.llvm.targetTriple, closedWorld = context.llvmModuleSpecification.isFinal)
    LlvmOptimizationPipeline(config, context.llvm.module, context).use {
        it.run()
    }
}

internal val CoveragePhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "Coverage",
        description = "Produce coverage information",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ -> runCoveragePass(context) }
)

internal val RemoveRedundantSafepointsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "RemoveRedundantSafepoints",
        description = "Remove function prologue safepoints inlined to another function",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ ->
            RemoveRedundantSafepointsPass().runOnModule(
                    module = context.llvm.module,
                    isSafepointInliningAllowed = context.shouldInlineSafepoints()
            )
        }
)

internal val OptimizeTLSDataLoadsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "OptimizeTLSDataLoads",
        description = "Optimize multiple loads of thread data",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ -> removeMultipleThreadDataLoads(context) }
)

internal val CStubsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, Unit>(
        name = "CStubs",
        description = "C stubs compilation",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, _ -> produceCStubs(context) }
)

internal val LinkBitcodeDependenciesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, List<File>>(
        name = "LinkBitcodeDependencies",
        description = "Link bitcode dependencies",
        postactions = getDefaultLlvmModuleActions(),
        op = { context, input -> linkBitcodeDependencies(context, input) }
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