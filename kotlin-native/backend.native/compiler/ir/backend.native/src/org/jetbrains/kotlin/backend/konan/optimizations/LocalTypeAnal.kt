/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.utils.copy
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DirectedGraphCondensationBuilder
import org.jetbrains.kotlin.backend.konan.DirectedGraphMultiNode
import org.jetbrains.kotlin.backend.konan.ir.actualCallee
import org.jetbrains.kotlin.backend.konan.ir.isAny
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.backend.konan.logMultiple
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.withNullability
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer
import java.util.*

sealed interface TypeApprox
object NoValue : TypeApprox
data class TypePoint(val type: IrType) : TypeApprox {
    init {
        require(!type.isInterface())
    }
}
data class TypeCone(val type: IrType) : TypeApprox

data class TypeFilter(val value: IrExpression, val thenFilter: TypeApprox, val elseFilter: TypeApprox)

interface SemilatticeWithBottom<T> : Semilattice<T> {
    val bottom: T
}

interface Lattice<T> : SemilatticeWithBottom<T> {
    fun join(x: T, y: T): T
}

internal class LocalTypeAnalOpt(val context: Context) : BodyLoweringPass {
    val typeSystem = context.typeSystem

    fun lcaNonNull(a: IrClass, b: IrClass): IrClass {
        if (a.isAny() || b.isAny()) return a
        if (a.defaultType in b.superTypes) return a
        val sa = a.superClass ?: return typeSystem.anyType().getClass()!!
        return lcaNonNull(sa, b)
    }
    fun lca(a: IrType, b: IrType): IrType {
        val hasNull = a.isNullable() || b.isNullable()
        val ca = a.getClass() ?: return typeSystem.anyType().withNullability(hasNull)
        val cb = b.getClass() ?: return typeSystem.anyType().withNullability(hasNull)
        return lcaNonNull(ca, cb).defaultType.withNullability(hasNull)
    }
    fun join(a: IrType, b: IrType): IrType? {
        val lca = lca(a, b)
        if (lca == a) return a
        if (lca == b) return b
        if (a.isInterface()) return a
        if (b.isInterface()) return b
        return null
    }
    operator fun TypeCone.contains(other: TypeApprox): Boolean = when (other) {
        NoValue -> true
        is TypePoint -> other.type.isSubtypeOfClass(this.type.classOrFail)
        is TypeCone -> other.type.isSubtypeOfClass(this.type.classOrFail)
    }
    val taLattice = object : Lattice<TypeApprox> {
        override val top: TypeApprox = NoValue
        override val bottom: TypeApprox = TypeCone(typeSystem.anyType().withNullability(true))
        override fun meet(x: TypeApprox, y: TypeApprox): TypeApprox {
            if (x == y) return x
            return when (x) {
                NoValue -> y
                is TypePoint -> when (y) {
                    NoValue -> x
                    is TypePoint -> TypeCone(lca(x.type, y.type))
                    is TypeCone -> if (x in y) y else TypeCone(lca(x.type, y.type))
                }
                is TypeCone -> when (y) {
                    NoValue -> x
                    is TypePoint -> TypeCone(lca(x.type, y.type))
                    is TypeCone -> if (x in y) y else TypeCone(lca(x.type, y.type))
                }
            }
        }
        override fun join(x: TypeApprox, y: TypeApprox): TypeApprox {
            if (x == y) return x
            return when (x) {
                NoValue -> NoValue
                is TypePoint -> when (y) {
                    NoValue -> NoValue
                    is TypePoint -> join(x.type, y.type)?.let { TypePoint(it) } ?: NoValue
                    is TypeCone -> if (x in y) x else NoValue
                }
                is TypeCone -> when (y) {
                    NoValue -> NoValue
                    is TypePoint -> if (y in x) y else NoValue
                    is TypeCone -> join(x.type, y.type)?.let { TypeCone(it) } ?: NoValue
                }
            }
        }
    }

    val valuesLattice = object : Lattice<Map<IrElement, TypeApprox>> {
        override val top = mapOf<IrElement, TypeApprox>()
        override val bottom = TODO()
        override fun meet(x: Map<IrElement, TypeApprox>, y: Map<IrElement, TypeApprox>): Map<IrElement, TypeApprox> {
            return (x.keys + y.keys).associateWith {
                taLattice.meet(
                        x[it] ?: NoValue,
                        y[it] ?: NoValue
                )
            }
        }
        override fun join(x: Map<IrElement, TypeApprox>, y: Map<IrElement, TypeApprox>): Map<IrElement, TypeApprox> {
            // FIXME?
            return (x.keys + y.keys).associateWith {
                taLattice.join(
                        x[it] ?: NoValue,
                        y[it] ?: NoValue
                )
            }
        }
    }

    fun IrExpression.matchAsFilter(): TypeFilter? {
        this as? IrTypeOperatorCall ?: return null
        if (this.operator == IrTypeOperator.INSTANCEOF) {
            // TODO can do better in else
            return TypeFilter(this.argument, TypeCone(this.typeOperand), taLattice.bottom)
        }
        if (this.operator == IrTypeOperator.NOT_INSTANCEOF) {
            // TODO can do better in else
            return TypeFilter(this.argument, taLattice.bottom, TypeCone(this.typeOperand))
        }
        return null
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // TODO val valueNumbers = mutableMapOf<IrElement, Int>()

        if (container is IrDeclarationWithName && container.name.asString().contains("haitest")) {
            val typeAnalysis = object : ForwardDataFlowAnalysis<Map<IrElement, TypeApprox>>(valuesLattice) {
                override fun visitWhen(expression: IrWhen, data: Map<IrElement, TypeApprox>): Map<IrElement, TypeApprox> {
                    val conditions = expression.branches.map { it.condition }
                    val conditionResults = conditions.fold(listOf<Map<IrElement, TypeApprox>>()) { acc, next ->
                        val prev = if (acc.isEmpty()) data else acc.last()
                        acc + listOf(next.accept(this, prev))
                    }
                    val conditionFilters = expression.branches.map { it.condition.matchAsFilter() }
                    val branchInputs = conditionResults.zip(conditionFilters) { conditionResult, conditionFilter ->
                        conditionFilter?.let {
                            // TODO what about else?
                            valuesLattice.join(conditionResult, mapOf(it.value to it.thenFilter))
                        } ?: conditionResult
                    }
                    val branchResults = branchInputs.zip(expression.branches) { branchInput, branch ->
                        branch.result.accept(this, branchInput)
                    }
                    val elseResult = if (!expression.branches.last().isUnconditional()) conditionResults.last() else lattice.top
                    return lattice.meetAll(branchResults + elseResult)
                }

                override fun visitConstructorCall(expression: IrConstructorCall, data: Map<IrElement, TypeApprox>): Map<IrElement, TypeApprox> {
                    return super.visitConstructorCall(expression, data) + mapOf(

                    )
                }
                override fun visitCall(expression: IrCall, data: Map<IrElement, TypeApprox>): Map<IrElement, TypeApprox> {

                }
            }

            container.accept(typeAnalysis)

            println()
            println(typeAnalysis.returnedStates)
            println()
        }
    }

}
