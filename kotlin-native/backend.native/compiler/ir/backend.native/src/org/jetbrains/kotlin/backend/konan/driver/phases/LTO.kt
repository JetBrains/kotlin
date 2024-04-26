/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.ir.GlobalHierarchyAnalysis
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.backend.konan.optimizations.DevirtualizationAnalysis
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFG
import org.jetbrains.kotlin.backend.konan.optimizations.ModuleDFGBuilder
import org.jetbrains.kotlin.backend.konan.optimizations.dce
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal val GHAPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "GHAPhase",
        description = "Global hierarchy analysis",
        op = { generationState, irModule ->
            GlobalHierarchyAnalysis(generationState.context, irModule).run()
        }
)

internal val BuildDFGPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment, ModuleDFG>(
        name = "BuildDFG",
        description = "Data flow graph building",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        outputIfNotEnabled = { _, _, generationState, _ ->
            val context = generationState.context
            val symbolTable = DataFlowIR.SymbolTable(context, DataFlowIR.Module())
            ModuleDFG(mutableMapOf(), symbolTable)
        },
        op = { generationState, irModule ->
            ModuleDFGBuilder(generationState, irModule).build()
        }
)

internal data class DevirtualizationAnalysisInput(
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val DevirtualizationAnalysisPhase = createSimpleNamedCompilerPhase<NativeGenerationState, DevirtualizationAnalysisInput, DevirtualizationAnalysis.AnalysisResult>(
        name = "DevirtualizationAnalysis",
        description = "Devirtualization analysis",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        outputIfNotEnabled = { _, _, _, _ ->
            DevirtualizationAnalysis.AnalysisResult(
                    mutableMapOf(),
                    DevirtualizationAnalysis.DevirtualizationAnalysisImpl.EmptyTypeHierarchy
            )
        },
        op = { generationState, (irModule, moduleDFG) ->
            DevirtualizationAnalysis.run(generationState.context, irModule, moduleDFG)
        }
)

internal data class BackendInlinerInput(
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
        val devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult,
        val options: BackendInlinerOptions,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val BackendInlinerPhase = createSimpleNamedCompilerPhase<NativeGenerationState, BackendInlinerInput>(
        name = "BackendInliner",
        description = "Backend inliner",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, (irModule, moduleDFG, devirtualizationAnalysisResult, options) ->
            val context = generationState.context
            val callGraph = CallGraphBuilder(
                    context,
                    irModule,
                    moduleDFG,
                    DevirtualizationAnalysis.AnalysisResult(mutableMapOf(), devirtualizationAnalysisResult.typeHierarchy),
                    devirtualizedCallSitesUnfoldFactor = 0,
                    nonDevirtualizedCallSitesUnfoldFactor = 0,
            ).build()
            BackendInliner(generationState, moduleDFG, devirtualizationAnalysisResult.devirtualizedCallSites, callGraph, options).run()
        }
)

internal data class DCEInput(
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
        val devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val DCEPhase = createSimpleNamedCompilerPhase<NativeGenerationState, DCEInput, Set<IrFunction>?>(
        name = "DCEPhase",
        description = "Dead code elimination",
        outputIfNotEnabled = { _, _, _, _ -> null },
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            val context = generationState.context
            dce(context, input.irModule, input.moduleDFG, input.devirtualizationAnalysisResult)
        }
)

internal data class DevirtualizationInput(
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
        val devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val DevirtualizationPhase = createSimpleNamedCompilerPhase<NativeGenerationState, DevirtualizationInput>(
        name = "Devirtualization",
        description = "Devirtualization",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            DevirtualizationAnalysis.devirtualize(input.irModule, input.moduleDFG, generationState, input.devirtualizationAnalysisResult.devirtualizedCallSites,
                    DevirtualizationUnfoldFactors.IR_DEVIRTUALIZED_VTABLE_CALL, DevirtualizationUnfoldFactors.IR_DEVIRTUALIZED_ITABLE_CALL)
        }
)

internal data class EscapeAnalysisInput(
        val irModule: IrModuleFragment,
        val moduleDFG: ModuleDFG,
        val devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val EscapeAnalysisPhase = createSimpleNamedCompilerPhase<NativeGenerationState, EscapeAnalysisInput, Map<IrElement, Lifetime>>(
        name = "EscapeAnalysis",
        description = "Escape analysis",
        outputIfNotEnabled = { _, _, _, _ -> emptyMap() },
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            val lifetimes = mutableMapOf<IrElement, Lifetime>()
            val context = generationState.context
            val entryPoint = context.ir.symbols.entryPoint?.owner
            val nonDevirtualizedCallSitesUnfoldFactor =
                    if (entryPoint != null) {
                        // For a final program it can be safely assumed that what classes we see is what we got,
                        // so can take those. In theory we can always unfold call sites using type hierarchy, but
                        // the analysis might converge much, much slower, so take only reasonably small for now.
                        DevirtualizationUnfoldFactors.DFG_NON_DEVIRTUALIZED_CALL
                    } else {
                        // Can't tolerate any non-devirtualized call site for a library.
                        // TODO: What about private virtual functions?
                        // Note: 0 is also bad - this means that there're no inheritors in the current source set,
                        // but there might be some provided by the users of the library being produced.
                        -1
                    }
            val callGraph = CallGraphBuilder(
                    context,
                    input.irModule,
                    input.moduleDFG,
                    input.devirtualizationAnalysisResult,
                    DevirtualizationUnfoldFactors.DFG_DEVIRTUALIZED_CALL,
                    nonDevirtualizedCallSitesUnfoldFactor
            ).build()
            EscapeAnalysis.computeLifetimes(context, generationState, input.moduleDFG, callGraph, lifetimes)
            lifetimes
        }
)

internal data class RedundantCallsInput(
        val moduleDFG: ModuleDFG,
        val devirtualizationAnalysisResult: DevirtualizationAnalysis.AnalysisResult,
        val irModule: IrModuleFragment,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val RemoveRedundantCallsToStaticInitializersPhase = createSimpleNamedCompilerPhase<NativeGenerationState, RedundantCallsInput>(
        name = "RemoveRedundantCallsToStaticInitializersPhase",
        description = "Redundant static initializers calls removal",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        op = { generationState, input ->
            val context = generationState.context
            val moduleDFG = input.moduleDFG

            val callGraph = CallGraphBuilder(
                    context,
                    input.irModule,
                    moduleDFG,
                    input.devirtualizationAnalysisResult,
                    devirtualizedCallSitesUnfoldFactor = Int.MAX_VALUE,
                    nonDevirtualizedCallSitesUnfoldFactor = Int.MAX_VALUE
            ).build()

            val rootSet = DevirtualizationAnalysis.computeRootSet(context, input.irModule, moduleDFG)
                    .mapNotNull { it.irFunction }
                    .toSet()

            StaticInitializersOptimization.removeRedundantCalls(context, input.irModule, callGraph, rootSet)
        }
)