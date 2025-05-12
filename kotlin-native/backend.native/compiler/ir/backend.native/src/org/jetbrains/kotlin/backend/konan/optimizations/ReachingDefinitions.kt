/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol


data class VarDefinition(val variable: IrValueSymbol, val definition: IrElement)
private typealias DefinitionsSet = BitSubsets<VarDefinition>.Subset

abstract class ReachingDefinitions private constructor(val transitive: Boolean, val subsets: BitSubsets<VarDefinition>) :
        ForwardDataFlowAnalysis<DefinitionsSet>(subsets.unionSemilattice()) {

    constructor(transitive: Boolean): this(transitive, BitSubsets<VarDefinition>())

    private fun handleAssign(definition: VarDefinition, assignedValue: IrExpression?, knownDefs: DefinitionsSet): DefinitionsSet {
        val transitiveDefinitions = if (transitive && assignedValue is IrGetValue) {
            val defsOfAssigned = knownDefs.filter { it.variable == assignedValue.symbol }.map { it.definition }
            defsOfAssigned.map { VarDefinition(definition.variable, it) }
        } else emptyList()
        val newDefinitions = transitiveDefinitions.ifEmpty { listOf(definition) }
        return subsets.Subset(knownDefs.filter { it.variable != definition.variable } + newDefinitions)
    }

    override fun visitVariable(declaration: IrVariable, data: DefinitionsSet): DefinitionsSet {
        val childrenResult = super.visitVariable(declaration, data)
        val init = declaration.initializer
        if (declaration.isVar && init != null) {
            return handleAssign(
                    VarDefinition(declaration.symbol, declaration),
                    init,
                    childrenResult
            )
        }
        return childrenResult
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: DefinitionsSet): DefinitionsSet {
        return handleAssign(
                VarDefinition(declaration.symbol, declaration),
                declaration.defaultValue?.expression,
                super.visitValueParameter(declaration, data)
        )
    }

    override fun visitSetValue(expression: IrSetValue, data: DefinitionsSet): DefinitionsSet {
        val childrenResult = super.visitSetValue(expression, data)
        return handleAssign(
                VarDefinition(expression.symbol as IrVariableSymbol, expression),
                expression.value,
                childrenResult
        )
    }
}

fun collectReachingDefinitions(body: IrFunction, transitive: Boolean): MutableMap<IrGetValue, List<IrElement>> {
    val result = mutableMapOf<IrGetValue, List<IrElement>>()
    val collector = object : ReachingDefinitions(transitive) {
        override fun visitGetValue(expression: IrGetValue, data: DefinitionsSet): DefinitionsSet {
            return super.visitGetValue(expression, data).also {
                val variable = expression.symbol.owner
                val reachingDefs = data.filter { it.variable == variable.symbol }.map { it.definition }
                result[expression] = reachingDefs
            }
        }
    }
    body.accept(collector, collector.lattice.top)
    return result
}
