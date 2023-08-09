/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt
import org.jetbrains.kotlin.formver.scala.toScalaBigInt
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * Convert a statement, emitting the resulting Viper statements and
 * declarations into the context, returning a reference to the
 * expression containing the result.  Note that in the FIR, expressions
 * are a subtype of statements.
 */
class StmtConversionVisitor : FirVisitor<Exp, StmtConversionContext>() {
    // Note that in some cases we don't expect to ever implement it: we are only
    // translating statements here, after all.  It isn't 100% clear how best to
    // communicate this.
    override fun visitElement(element: FirElement, data: StmtConversionContext): Exp =
        TODO("Not yet implemented for $element (${element.source.text})")

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: StmtConversionContext): Exp {
        val expr = returnExpression.result.accept(this, data)
        // TODO: respect return-based control flow
        val returnVar = data.methodCtx.returnVar
        data.statements.add(Stmt.LocalVarAssign(returnVar.toLocalVar(), expr))
        return UnitDomain.element
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext): Exp {
        // TODO: allow blocks to return values
        block.statements.forEach { it.accept(this, data) }
        return UnitDomain.element
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: StmtConversionContext): Exp =
        when (constExpression.kind) {
            ConstantValueKind.Int -> Exp.IntLit((constExpression.value as Long).toInt().toScalaBigInt())
            ConstantValueKind.Boolean -> Exp.BoolLit(constExpression.value as Boolean)
            else -> TODO("Constant Expression of type ${constExpression.kind} is not yet implemented.")
        }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: StmtConversionContext): Exp {
        if (whenExpression.usedAsExpression) {
            // The problem with conditional expressions is that Kotlin conditional expressions can have statements whereas Viper
            // conditionals can't. These need to be embedded as Viper statements before the IF, however, one has to carefully consider
            // short-circuiting semantics.
            TODO("Conditionals used as expressions are not yet implemented.")
        }

        if (whenExpression.branches.size != 2) {
            // For now only when expressions with 2 branches are supported so if statements can be encoded.
            // When expressions with more than 2 branches can be embedded as nested if-else chains.
            TODO("When expressions with more that two branches are not yet implemented")
        }

        when (whenExpression.subject) {
            null -> {
                val cond = whenExpression.branches[0].condition.accept(this, data)
                val thenCtx = StmtConversionContext(data.methodCtx)
                thenCtx.convertAndAppend(whenExpression.branches[0].result)
                val elseCtx = StmtConversionContext(data.methodCtx)
                elseCtx.convertAndAppend(whenExpression.branches[1].result)
                data.statements.add(
                    Stmt.If(
                        cond,
                        thenCtx.block,
                        elseCtx.block
                    )
                )
            }
            else -> TODO("Can't embed $whenExpression since when expressions with a subject other than null are not yet supported.")
        }
        return UnitDomain.element
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext,
    ): Exp {
        val symbol = propertyAccessExpression.calleeReference.toResolvedBaseSymbol()!!
        val type = propertyAccessExpression.typeRef.coneTypeOrNull!!
        return when (symbol) {
            is FirValueParameterSymbol -> ConvertedVar(
                symbol.callableId.convertName(),
                data.methodCtx.programCtx.convertType(type)
            ).toLocalVar()
            is FirPropertySymbol -> ConvertedVar(
                symbol.callableId.convertName(),
                data.methodCtx.programCtx.convertType(type)
            ).toLocalVar()
            else -> TODO("Implement other property accesses")
        }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext): Exp {
        val id = functionCall.calleeReference.toResolvedCallableSymbol()!!.callableId
        // TODO: figure out a more structured way of doing this
        if (id.packageName.asString() == "kotlin.contracts" && id.callableName.asString() == "contract") return UnitDomain.element
        return when (functionCall.dispatchReceiver) {
            is FirNoReceiverExpression -> {
                val symbol = functionCall.calleeReference.resolved!!.resolvedSymbol as FirNamedFunctionSymbol
                val calleeSig = data.methodCtx.programCtx.add(symbol)
                val args = functionCall.argumentList.arguments.map {
                    it.accept(this, data)
                }
                val returnVar = data.methodCtx.newAnonVar(calleeSig.returnType)
                val returnExp = returnVar.toLocalVar()
                data.declarations.add(returnVar.toLocalVarDecl())
                data.statements.add(Stmt.MethodCall(calleeSig.name.asString, args, listOf(returnExp)))
                returnExp
            }
            else -> TODO("Implement function call visitation with receiver")
        }
    }

    override fun visitProperty(property: FirProperty, data: StmtConversionContext): Exp {
        val symbol = property.symbol
        val type = property.returnTypeRef.coneTypeOrNull!!
        if (!symbol.isLocal) {
            TODO("Implement non-local properties")
        }
        val cvar = ConvertedVar(symbol.callableId.convertName(), data.methodCtx.programCtx.convertType(type))
        val propInitializer = property.initializer
        val initializer = propInitializer?.accept(this, data)
        data.declarations.add(cvar.toLocalVarDecl())
        initializer?.let { data.statements.add(Stmt.LocalVarAssign(cvar.toLocalVar(), it)) }
        return UnitDomain.element
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: StmtConversionContext): Exp {
        val cond = whileLoop.condition.accept(this, data)
        val invariants: List<Exp> = emptyList()
        val bodyStmtConversionContext = StmtConversionContext(data.methodCtx)
        bodyStmtConversionContext.convertAndAppend(whileLoop.block)
        val body = bodyStmtConversionContext.block
        data.statements.add(Stmt.While(cond, invariants, body))
        return UnitDomain.element
    }
}