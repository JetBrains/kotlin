/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.copy
import java.util.BitSet


interface Semilattice<T> {
    /** Identity element: (x meet top) == x */
    val top: T

    /**
     * Meet is what happens when control flow paths merge.
     * Must be idempotent, commutative and associative.
     */
    fun meet(x: T, y: T): T
    fun meetInPlace(x: T, y: T): T = meet(x, y)

    fun meetAllInPlace(values: List<T>): T = values.reduce { x, y -> meet(x, y) }

    fun copy(x: T): T
}


private fun <K, T> MutableMap<K, T>.meetOrInsert(key: K, lattice: Semilattice<T>, value: T) {
    getOrPut(key) { lattice.top }.let {
        lattice.meetInPlace(it, value)
    }
    //this[key] = lattice.meet(this[key] ?: lattice.top, value)
}

object BitsetUnionSemilattice : Semilattice<BitSet> {
    override val top: BitSet = BitSet()
    override fun meet(x: BitSet, y: BitSet): BitSet = x.copy().also { it.or(y) }
    override fun copy(x: BitSet): BitSet = x.copy()
}

class BitsetIntersectLattice(completeSet: BitSet) : Semilattice<BitSet> {
    override val top: BitSet = completeSet
    val bottom: BitSet = BitSet()
    override fun meet(x: BitSet, y: BitSet): BitSet = x.copy().also { it.and(y) }
    override fun meetInPlace(x: BitSet, y: BitSet): BitSet = x.also { it.and(y) }
    override fun meetAllInPlace(values: List<BitSet>): BitSet {
        val result = values.first()
        for (v in values.drop(1)) {
            meetInPlace(result, v)
        }
        return result
    }
    fun join(x: BitSet, y: BitSet): BitSet = x.copy().also { it.or(y) }
    override fun copy(x: BitSet): BitSet = x.copy()
}

abstract class ForwardDataFlowAnalysis<T>(val lattice: Semilattice<T>) : IrVisitor<T, T>() {

    val continuedStates = mutableMapOf<IrLoop, T>()
    val breakedStates = mutableMapOf<IrLoop, T>()
    val returnedStates = mutableMapOf<IrReturnTargetSymbol, T>()

    infix fun T.meet(other: T): T = lattice.meet(this, other)
    infix fun T.meetInPlace(other: T): T = lattice.meetInPlace(this, other)

    override fun visitElement(element: IrElement, data: T): T {
        var resultData = data
        element.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                resultData = element.accept(this@ForwardDataFlowAnalysis, resultData)
            }
        })
        return resultData
    }

    override fun visitClass(declaration: IrClass, data: T): T {
        return data;
    }

    override fun visitFunction(declaration: IrFunction, data: T): T {
        return data;
    }

    final override fun visitBranch(branch: IrBranch, data: T): T = error("Should not call this")

    override fun visitWhen(expression: IrWhen, data: T): T {
        val conditions = expression.branches.map { it.condition }
        val conditionResults = conditions.fold(listOf<T>()) { acc, next ->
            val prev = if (acc.isEmpty()) data else acc.last()
            acc + listOf(lattice.copy(next.accept(this, prev)))
        }
        val branchResults = conditionResults.zip(expression.branches) { conditionResult, branch ->
            branch.result.accept(this, conditionResult)
        }
        val elseResult = if (!expression.branches.last().isUnconditional()) conditionResults.last() else lattice.top
        return lattice.meetAllInPlace(branchResults + elseResult)
    }

    final override fun visitWhileLoop(loop: IrWhileLoop, data: T): T = super.visitWhileLoop(loop, data)
    final override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: T): T = super.visitDoWhileLoop(loop, data)

    override fun visitLoop(loop: IrLoop, data: T): T {
        continuedStates[loop] = lattice.top
        breakedStates[loop] = lattice.top

        var lastIterData = lattice.top
        // TODO consider short-circuiting in special cases
        while (true) {
            var thisIterData = data meetInPlace lastIterData

            thisIterData = thisIterData meetInPlace continuedStates[loop]!!

            if (loop is IrWhileLoop) {
                thisIterData = thisIterData meetInPlace loop.condition.accept(this, thisIterData)
            }

            thisIterData = loop.body?.accept(this, thisIterData) ?: thisIterData

            if (loop is IrDoWhileLoop) {
                thisIterData = thisIterData meetInPlace loop.condition.accept(this, thisIterData)
            }

            thisIterData = thisIterData meetInPlace breakedStates[loop]!!

            if (thisIterData == lastIterData) break
            //require(thisIterData meet lastIterData == thisIterData) { "Data flow analysis diverges" }
            lastIterData = thisIterData
        }

        return lastIterData
    }

    override fun visitContinue(jump: IrContinue, data: T): T {
        continuedStates.meetOrInsert(jump.loop, lattice, data)
        return data
    }

    override fun visitBreak(jump: IrBreak, data: T): T {
        breakedStates.meetOrInsert(jump.loop, lattice, data)
        return data
    }

    override fun visitReturnableBlock(expression: IrReturnableBlock, data: T): T {
        val blockResult = super.visitReturnableBlock(expression, data)
        return lattice.meetInPlace(blockResult, returnedStates[expression.symbol] ?: lattice.top)
    }

    override fun visitReturn(expression: IrReturn, data: T): T {
        val result = super.visitReturn(expression, data)
        returnedStates.meetOrInsert(expression.returnTargetSymbol, lattice, result)
        return lattice.top
    }

    override fun visitTry(aTry: IrTry, data: T): T {
        // We conservatively assume any possible execution here

        // The try block may be executed only partially,
        // the approximation of "either entirely or not at all" is safe.
        val afterTry = aTry.tryResult.accept(this, data) meetInPlace data
        val afterCatches = aTry.catches.map { it.result.accept(this, afterTry) }

        require(aTry.finallyExpression == null)

        return lattice.meetAllInPlace(listOf(afterTry) + afterCatches)
    }

    override fun visitThrow(expression: IrThrow, data: T): T {
        super.visitThrow(expression, data)
        return lattice.top
    }
}