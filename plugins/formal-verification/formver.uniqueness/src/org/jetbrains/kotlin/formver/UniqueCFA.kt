/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.utils.addToStdlib.popLast

class UniqueCFA(private val data: UniqueCheckerContext) : FirControlFlowChecker(MppCheckerKind.Common) {
    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, context: CheckerContext) {
        graph.traverseToFixedPoint(UniqueInfoCollector(this.data))
    }

    private class UniqueInfoCollector(private val context: UniqueCheckerContext) :
        PathAwareControlFlowGraphVisitor<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>() {

        override fun mergeInfo(
            a: ControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
            b: ControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
            node: CFGNode<*>,
        ): ControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>> {
            // TODO: No implemented yet, for now just return from one branch
            return a
        }

        override fun visitEdge(
            from: CFGNode<*>,
            to: CFGNode<*>,
            metadata: Edge,
            data: PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
        ): PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>> {
            val dataForEdge = super.visitEdge(from, to, metadata, data)
            return dataForEdge
        }

        override fun visitNode(
            node: CFGNode<*>,
            data: PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
        ): PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>> {
            if (node is EnterNodeMarker) {
                context.uniqueStack.add(ArrayDeque())
            } else if (node is ExitNodeMarker) {
                context.uniqueStack.popLast().lastOrNull()?.let { context.uniqueStack.last().add(it) }
            }
            return data
        }

        override fun visitFunctionEnterNode(
            node: FunctionEnterNode,
            data: PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
        ): PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>> {
            val dataForNode = visitNode(node, data)
            val declaration = node.fir

            // construct context and start control flow analysis
            if (declaration.receiverParameter != null) {
                TODO("Ignore receiver for now, not sure how to convert it into property symbol yet")
            }
            val valueParameters =
                declaration.valueParameters.associate {
                    Pair<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>(
                        it.symbol,
                        setOf(this.context.resolveUniqueAnnotation(it))
                    )
                }.toMap()
            return dataForNode.transformValues { it.putAll(valueParameters) }
        }

        override fun visitVariableDeclarationNode(
            node: VariableDeclarationNode,
            data: PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
        ): PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>> {
            val dataForNode = visitNode(node, data)
            val lSymbol = node.fir.symbol
            if (node.fir.initializer == null) {
                return dataForNode.transformValues { it.put(lSymbol, setOf(UniqueLevel.Shared)) }
            }
            val rhsUnique = when (val last = context.uniqueStack.last().last()) {
                is Level -> last.level
                is Path -> dataForNode[NormalPath]?.get(last.symbol) ?: setOf(UniqueLevel.Shared)
            }
            return dataForNode.transformValues { it.put(lSymbol, rhsUnique) }
        }

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
        ): PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>> {
            val dataForNode = visitNode(node, data)
            val lSymbol = node.fir.lValue.toResolvedCallableSymbol(context.session)
            val rhsUnique = when (val last = context.uniqueStack.last().last()) {
                is Level -> last.level
                is Path -> dataForNode[NormalPath]?.get(last.symbol) ?: setOf(UniqueLevel.Shared)
            }
            return dataForNode.transformValues { it.put(lSymbol as FirVariableSymbol, rhsUnique) }
        }

        @OptIn(SymbolInternals::class)
        override fun visitFunctionCallArgumentsExitNode(
            node: FunctionCallArgumentsExitNode,
            data: PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
        ): PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>> {
            // TODO: this is where function call parameter checking should happen
            // Here do not call visitNode
            val params = (node.fir.toResolvedCallableSymbol() as FirFunctionSymbol<*>).fir.valueParameters
            val argumentAliasOrUnique: MutableList<PathUnique> = mutableListOf()
            for (i in params.indices) {
                argumentAliasOrUnique.add(context.uniqueStack.last().popLast())
            }
            // FIXME: might not get annotation in this way
            params.zip(argumentAliasOrUnique).forEach { (param, varUnique) ->
                val argUniqueLevel = this.context.resolveUniqueAnnotation(param)
                val passedUnique: Set<UniqueLevel> = when (varUnique) {
                    is Path -> data[NormalPath]?.get(varUnique.symbol) ?: setOf(UniqueLevel.Shared)
                    is Level -> varUnique.level
                }
                // TODO: more general comparison
                if (!passedUnique.lessThanOrEqual(argUniqueLevel)) {
                    throw IllegalArgumentException("uniqueness level not match ${param.source.text}")
                }
            }
            return data
        }

        override fun visitQualifiedAccessNode(
            node: QualifiedAccessNode,
            data: PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
        ): PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>> {
            val dataForNode = visitNode(node, data)
            val callee = node.fir.calleeReference.symbol as FirVariableSymbol
            context.uniqueStack.last().add(Path(callee))
            return dataForNode
        }

        @OptIn(SymbolInternals::class)
        override fun visitFunctionCallExitNode(
            node: FunctionCallExitNode,
            data: PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>>,
        ): PathAwareControlFlowInfo<FirVariableSymbol<FirVariable>, Set<UniqueLevel>> {
            val uniqueLevel = mutableSetOf<UniqueLevel>()
            val symbol = node.fir.calleeReference.symbol
            if (symbol == null) {
                uniqueLevel.add(UniqueLevel.Shared)
            } else {
                uniqueLevel.add(this.context.resolveUniqueAnnotation(symbol.fir))
            }
            context.uniqueStack.last().add(Level(uniqueLevel))

            // Function call does not change context
            return data
        }
    }
}