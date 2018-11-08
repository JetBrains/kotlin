/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.cfg

import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.BlockScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.CallInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.*
import org.jetbrains.kotlin.contracts.contextual.declaredFactsInfo
import org.jetbrains.kotlin.contracts.contextual.model.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.javaslang.ImmutableHashMap

class PseudocodeEffectsData(
    pseudocode: Pseudocode,
    private val diagnosticSink: DiagnosticSink,
    private val bindingContext: BindingContext,
    private val declaredContracts: Map<ContextFamily, ContextContracts>
) {
    companion object {
        private const val NO_LEVEL = -1
    }

    private val myDiagnostics = mutableListOf<Pair<KtElement, String>>()
    val diagnostics: List<Pair<KtElement, String>>
        get() = myDiagnostics

    val resultingContexts: Map<ContextFamily, List<Context>>? = computeEffectsControlFlowInfo(pseudocode)


    private fun computeEffectsControlFlowInfo(pseudocode: Pseudocode): Map<ContextFamily, List<Context>>? {
        // collect info via CFA
        val edgesMap = pseudocode.collectData(
            traversalOrder = TraversalOrder.FORWARD,
            mergeEdges = ::mergeEdges,
            updateEdge = ::updateEdge,
            initialInfo = ContractsContextsInfo.EMPTY,
            localFunctionAnalysisStrategy = LocalFunctionAnalysisStrategy.ONLY_IN_PLACE_LAMBDAS
        )

        // verify resulting context
        pseudocode.traverse(
            traversalOrder = TraversalOrder.FORWARD,
            edgesMap = edgesMap,
            analyzeInstruction = ::verifyInstruction,
            analyzeIncomingEdge = ::verifyIncomingEdge,
            localFunctionAnalysisStrategy = LocalFunctionAnalysisStrategy.ONLY_IN_PLACE_LAMBDAS
        )

        val contextsGroupedByFamily = edgesMap[pseudocode.exitInstruction]?.incoming?.toMutableMap() ?: return null
        return contextsGroupedByFamily.mapValues { (_, contextsGropedByLevel) -> contextsGropedByLevel.map { it.value } }
    }

    // -------------------------- Verification --------------------------

    private fun verifyInstruction(
        instruction: Instruction,
        info: ContractsContextsInfo,
        outgoing: ContractsContextsInfo
    ) {
        if (instruction !is CallInstruction) return
        val callExpression = instruction.element as? KtCallExpression ?: return
        val verifiers = callExpression.declaredFactsInfo(bindingContext).verifiers
        verifyContext(verifiers, info)
    }

    private fun verifyIncomingEdge(
        previousInstruction: Instruction,
        instruction: Instruction,
        info: ContractsContextsInfo
    ) {
        val previousDepth = previousInstruction.blockScope.depth
        val currentDepth = instruction.blockScope.depth
        if (previousDepth > currentDepth) {
            val block = instruction.blockScope.block as? KtExpression ?: return
            val verifiers = block.declaredFactsInfo(bindingContext).verifiers
            verifyContext(verifiers, info)
        }
    }

    private fun verifyContext(verifiers: Collection<ContextVerifier>, info: ContractsContextsInfo) {
        for (verifier in verifiers) {
            val family = verifier.family
            val contextsGroupedByLevel = info[family].map { it.toMutableMap() }.getOrElse(mutableMapOf())

            if (NO_LEVEL !in contextsGroupedByLevel) {
                contextsGroupedByLevel[NO_LEVEL] = family.emptyContext
            }
            val contexts = contextsGroupedByLevel.toList().sortedBy { (depth, _) -> depth }.map { it.second }

            verifier.verify(contexts, diagnosticSink, declaredContracts[family] ?: ContextContracts())
        }
    }

    // -------------------------- Collecting data (updateEdge) --------------------------

    private fun updateEdge(
        previousInstruction: Instruction,
        instruction: Instruction,
        info: ContractsContextsInfo
    ): ContractsContextsInfo {
        val previousDepth = previousInstruction.blockScope.depth
        val currentDepth = instruction.blockScope.depth

        return when {
            previousDepth < currentDepth -> visitBlockEnter(previousInstruction.blockScope, info)
            previousDepth > currentDepth -> visitBlockExit(instruction.blockScope, info)
            else -> info
        }
    }

    // collect facts
    private fun visitBlockEnter(
        blockScope: BlockScope,
        info: ContractsContextsInfo
    ): ContractsContextsInfo {
        val block = blockScope.block as? KtExpression ?: return info
        val providers = block.declaredFactsInfo(bindingContext).providers
        val contextsGroupedByFamily = info.toMutableMap()
        applyProviders(contextsGroupedByFamily, providers, blockScope.depth)
        return ContractsContextsInfo(contextsGroupedByFamily)
    }

    // collect checkers
    private fun visitBlockExit(
        blockScope: BlockScope,
        info: ContractsContextsInfo
    ): ContractsContextsInfo {
        val block = blockScope.block as? KtExpression ?: return info
        val cleaners = block.declaredFactsInfo(bindingContext).cleaners
        val depth = blockScope.depth
        val context = info.toMutableMap()
        for (family in context.keys) {
            context[family]!!.remove(depth)
        }
        applyCleaners(cleaners, context)
        return ContractsContextsInfo(context)
    }

    // -------------------------- Collecting data (mergeEdges) --------------------------

    private fun mergeEdges(
        instruction: Instruction,
        incoming: Collection<ContractsContextsInfo>
    ): Edges<ContractsContextsInfo> {
        val mergedData = merge(incoming)
        val updatedData = if (instruction is CallInstruction) visitCallInstruction(instruction, mergedData) else mergedData
        return Edges(mergedData, updatedData)
    }

    private fun merge(incoming: Collection<ContractsContextsInfo>): ContractsContextsInfo {
        when (incoming.size) {
            0 -> return ContractsContextsInfo.EMPTY
            1 -> return incoming.first()
        }
        val families = incoming.flatMap { it.keySet() }.toSet()

        val convertedIncoming = incoming.map { it.convertToMap() }

        val reducedContextsGroupedByFamily = mutableMapOf<ContextFamily, Map<Int, Context>>()
        for (family in families) {
            val familyContextsGroupedByDepth = convertedIncoming.map { it[family] }
            val depths = familyContextsGroupedByDepth.filterNotNull().flatMap { it.keys }.toSet()

            val incomingContextsGroupedByDepth = mutableMapOf<Int, List<Context>>()
            for (depth in depths) {
                val contexts = familyContextsGroupedByDepth.map { it?.get(depth) ?: family.emptyContext }
                incomingContextsGroupedByDepth[depth] = contexts
            }

            val reducedContextsGroupedByLevel = incomingContextsGroupedByDepth
                .mapValues { (_, incomingContexts) -> incomingContexts.reduce(family.combiner::or) }

            reducedContextsGroupedByFamily[family] = reducedContextsGroupedByLevel
        }

        return ContractsContextsInfo(ImmutableHashMap.ofAll(reducedContextsGroupedByFamily))
    }

    // collect facts and checkers
    private fun visitCallInstruction(
        instruction: CallInstruction,
        info: ContractsContextsInfo
    ): ContractsContextsInfo {
        val contextsGroupedByFamily = info.toMutableMap()

        val callExpression = instruction.element as? KtCallExpression ?: return info

        val (providers, _, cleaners) = callExpression.declaredFactsInfo(bindingContext)
        applyProviders(contextsGroupedByFamily, providers, null)
        applyCleaners(cleaners, contextsGroupedByFamily)

        return ContractsContextsInfo(contextsGroupedByFamily)
    }

    // TODO: contexts to Map<>
    private fun applyProviders(
        contextsGroupedByFamily: MutableMap<ContextFamily, MutableMap<Int, Context>>,
        providers: Collection<ContextProvider>,
        depth: Int?
    ) {
        val level = depth ?: NO_LEVEL
        for (provider in providers) {
            val family = provider.family
            if (family !in contextsGroupedByFamily) {
                contextsGroupedByFamily[family] = mutableMapOf()
            }
            val existedContext = contextsGroupedByFamily[family]!![level] ?: family.emptyContext
            contextsGroupedByFamily[family]!![level] = family.combiner.combine(existedContext, provider)
        }
    }

    private fun applyCleaners(
        cleaners: Collection<ContextCleaner>,
        contextsGroupedByFamily: MutableMap<ContextFamily, MutableMap<Int, Context>>
    ) {
        for (cleaner in cleaners) {
            val family = cleaner.family
            if (family !in contextsGroupedByFamily) {
                contextsGroupedByFamily[family] = mutableMapOf()
            }

            contextsGroupedByFamily[family] = contextsGroupedByFamily[family]!!.mapValues { (_, context) ->
                cleaner.cleanupProcessed(context)
            }.toMutableMap()
        }
    }

    private fun ContractsContextsInfo.convertToMap() = iterator().map { it._1 to it._2 }.toList().toMap()

    private fun ContractsContextsInfo.toMutableMap() = convertToMap()
        .mapValues { (_, map) -> map.toMutableMap() }
        .toMutableMap()
}