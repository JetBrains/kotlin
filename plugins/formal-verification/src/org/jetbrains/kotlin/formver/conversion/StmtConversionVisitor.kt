/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.contracts.description.LogicOperationKind
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
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.domains.NullableDomain
import org.jetbrains.kotlin.formver.domains.UnitDomain
import org.jetbrains.kotlin.formver.embeddings.BooleanTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.embedName
import org.jetbrains.kotlin.formver.scala.silicon.ast.Exp
import org.jetbrains.kotlin.formver.scala.silicon.ast.Stmt
import org.jetbrains.kotlin.formver.scala.silicon.ast.Type
import org.jetbrains.kotlin.formver.scala.toScalaBigInt
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * Convert a statement, emitting the resulting Viper statements and
 * declarations into the context, returning a reference to the
 * expression containing the result.  Note that in the FIR, expressions
 * are a subtype of statements.
 *
 * In many cases, we introduce a temporary variable to represent this
 * result (since, for example, a method call is not an expression).
 * When the result is an lvalue, it is important to return an expression
 * that refers to location, not just the same value, and so introducing
 * a temporary variable for the result is not acceptable in those cases.
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
        val returnVar = data.signature.returnVar
        data.addStatement(Stmt.LocalVarAssign(returnVar.toLocalVar(), expr))
        return UnitDomain.element
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext): Exp =
        // We ignore the accumulator: we just want to get the result of the last expression.
        block.statements.fold(UnitDomain.element) { _, it -> it.accept(this, data) }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: StmtConversionContext): Exp =
        when (constExpression.kind) {
            ConstantValueKind.Int -> Exp.IntLit((constExpression.value as Long).toInt().toScalaBigInt())
            ConstantValueKind.Boolean -> Exp.BoolLit(constExpression.value as Boolean)
            /* TODO: For now null is always hard-coded to be of type Nullable[Int].
             * This needs to be generalized and for this the type of the return expressions needs to be known.
             * This type should maybe be passed as function argument.
             */
            ConstantValueKind.Null -> NullableDomain.nullVal(Type.Int)
            else -> TODO("Constant Expression of type ${constExpression.kind} is not yet implemented.")
        }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: StmtConversionContext): Exp {
        if (whenExpression.branches.size != 2) {
            // For now only when expressions with 2 branches are supported so if statements can be encoded.
            // When expressions with more than 2 branches can be embedded as nested if-else chains.
            TODO("When expressions with more that two branches are not yet implemented")
        }

        return when (whenExpression.subject) {
            null -> {
                val cvar = data.newAnonVar(data.embedType(whenExpression.typeRef.coneTypeOrNull!!))
                val cond = whenExpression.branches[0].condition.accept(this, data)
                // TODO: tidy this up and split it into helper functions.
                val thenCtx = StmtConverter(data)
                val thenResult = whenExpression.branches[0].result.accept(this, thenCtx)
                thenCtx.addStatement(Stmt.LocalVarAssign(cvar.toLocalVar(), thenResult))
                val elseCtx = StmtConverter(data)
                val elseResult = whenExpression.branches[1].result.accept(this, elseCtx)
                elseCtx.addStatement(Stmt.LocalVarAssign(cvar.toLocalVar(), elseResult))
                data.addDeclaration(cvar.toLocalVarDecl())
                data.addStatement(
                    Stmt.If(
                        cond,
                        thenCtx.block,
                        elseCtx.block
                    )
                )
                cvar.toLocalVar()
            }
            else -> TODO("Can't embed $whenExpression since when expressions with a subject other than null are not yet supported.")
        }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext,
    ): Exp {
        val symbol = propertyAccessExpression.calleeReference.toResolvedBaseSymbol()!!
        val type = propertyAccessExpression.typeRef.coneTypeOrNull!!
        return when (symbol) {
            is FirValueParameterSymbol -> VariableEmbedding(
                symbol.callableId.embedName(),
                data.embedType(type)
            ).toLocalVar()
            is FirPropertySymbol -> VariableEmbedding(
                symbol.callableId.embedName(),
                data.embedType(type)
            ).toLocalVar()
            else -> TODO("Implement other property accesses")
        }
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: StmtConversionContext): Exp {
        if (equalityOperatorCall.arguments.size != 2) {
            throw IllegalArgumentException("Invalid equality comparison $equalityOperatorCall, can only compare 2 elements.")
        }
        val left = equalityOperatorCall.arguments[0].accept(this, data)
        val right = equalityOperatorCall.arguments[1].accept(this, data)

        return when (equalityOperatorCall.operation) {
            FirOperation.EQ -> Exp.EqCmp(left, right)
            FirOperation.NOT_EQ -> Exp.NeCmp(left, right)
            else -> TODO("Equality comparison operation ${equalityOperatorCall.operation} not yet implemented.")
        }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext): Exp {
        val id = functionCall.calleeReference.toResolvedCallableSymbol()!!.callableId
        val specialFunc = SpecialFunctions.byCallableId[id]
        val getArgs = { getFunctionCallArguments(functionCall).map { it.accept(this, data) } }
        if (specialFunc != null) {
            if (specialFunc !is SpecialFunctionImplementation) return UnitDomain.element
            return specialFunc.convertCall(getArgs(), data)
        }

        val args = getArgs()
        val symbol = functionCall.calleeReference.resolved!!.resolvedSymbol as FirNamedFunctionSymbol
        val calleeSig = data.add(symbol)
        val returnVar = data.newAnonVar(calleeSig.returnType)
        val returnExp = returnVar.toLocalVar()
        data.addDeclaration(returnVar.toLocalVarDecl())
        data.addStatement(Stmt.MethodCall(calleeSig.name.mangled, args, listOf(returnExp)))
        return returnExp
    }

    private fun getFunctionCallArguments(functionCall: FirFunctionCall): List<FirExpression> {
        // I'm sure there's a nicer and more functional way of writing this, feel free to
        // refactor if you know how. :)
        val receiver = if (functionCall.dispatchReceiver !is FirNoReceiverExpression) {
            listOf(functionCall.dispatchReceiver)
        } else {
            emptyList()
        }
        return receiver + functionCall.argumentList.arguments
    }

    override fun visitProperty(property: FirProperty, data: StmtConversionContext): Exp {
        val symbol = property.symbol
        val type = property.returnTypeRef.coneTypeOrNull!!
        if (!symbol.isLocal) {
            TODO("Implement non-local properties")
        }
        val cvar = VariableEmbedding(symbol.callableId.embedName(), data.embedType(type))
        val propInitializer = property.initializer
        val initializer = propInitializer?.accept(this, data)
        data.addDeclaration(cvar.toLocalVarDecl())
        initializer?.let { data.addStatement(Stmt.LocalVarAssign(cvar.toLocalVar(), it)) }
        return UnitDomain.element
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: StmtConversionContext): Exp {
        val cond = whileLoop.condition.accept(this, data)
        val invariants: List<Exp> = emptyList()
        val bodyStmtConversionContext = StmtConverter(data)
        bodyStmtConversionContext.convertAndAppend(whileLoop.block)
        val body = bodyStmtConversionContext.block
        data.addStatement(Stmt.While(cond, invariants, body))
        return UnitDomain.element
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: StmtConversionContext): Exp {
        // It is not entirely clear whether we can get away with ignoring the distinction between
        // lvalues and rvalues, but let's try to at first, and we'll fix it later if it turns out
        // not to work.
        val convertedLValue = variableAssignment.lValue.accept(this, data)
        val convertedRValue = variableAssignment.rValue.accept(this, data)
        data.addStatement(Stmt.assign(convertedLValue, convertedRValue))
        return UnitDomain.element
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: StmtConversionContext): Exp {
        val oldType = smartCastExpression.originalExpression.typeRef.coneType
        val newType = smartCastExpression.smartcastType.coneType
        if (oldType.isNullable && !newType.isNullable) {
            val exp = smartCastExpression.originalExpression.accept(this, data)
            return NullableDomain.valOfApp(exp, data.embedType(newType).type)
        }
        TODO("Handle other kinds of smart casts.")
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: StmtConversionContext): Exp {
        val returnVar = data.newAnonVar(BooleanTypeEmbedding)
        data.addDeclaration(returnVar.toLocalVarDecl())
        val left = binaryLogicExpression.leftOperand.accept(this, data)
        val rightSubStmt = StmtConverter(data)
        val right = binaryLogicExpression.rightOperand.accept(this, rightSubStmt)
        rightSubStmt.addStatement(Stmt.LocalVarAssign(returnVar.toLocalVar(), right))
        when (binaryLogicExpression.kind) {
            LogicOperationKind.AND ->
                data.addStatement(
                    Stmt.If(
                        left,
                        rightSubStmt.block,
                        Stmt.Seqn(listOf(Stmt.LocalVarAssign(returnVar.toLocalVar(), Exp.BoolLit(false))), listOf())
                    )
                )
            LogicOperationKind.OR ->
                data.addStatement(
                    Stmt.If(
                        left,
                        Stmt.Seqn(listOf(Stmt.LocalVarAssign(returnVar.toLocalVar(), Exp.BoolLit(true))), listOf()),
                        rightSubStmt.block
                    )
                )
        }
        return returnVar.toLocalVar()
    }
}