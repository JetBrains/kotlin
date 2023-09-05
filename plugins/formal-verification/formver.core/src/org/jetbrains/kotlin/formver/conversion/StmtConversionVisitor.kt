/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.domains.TypeDomain
import org.jetbrains.kotlin.formver.domains.TypeOfDomain
import org.jetbrains.kotlin.formver.domains.UnitDomain
import org.jetbrains.kotlin.formver.domains.convertType
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.AccessPredicate
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
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
object StmtConversionVisitor : FirVisitor<Exp, StmtConversionContext<ResultTrackingContext>>() {
    // Note that in some cases we don't expect to ever implement it: we are only
    // translating statements here, after all.  It isn't 100% clear how best to
    // communicate this.
    override fun visitElement(element: FirElement, data: StmtConversionContext<ResultTrackingContext>): Exp =
        handleUnimplementedElement("Not yet implemented for $element (${element.source.text})", data)

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: StmtConversionContext<ResultTrackingContext>): Exp {
        val expr = data.convert(returnExpression.result)
        val exprType = data.embedType(returnExpression.result)
        data.addStatement(Stmt.LocalVarAssign(data.returnVar.toLocalVar(), expr.convertType(exprType, data.returnVar.type)))
        data.addStatement(data.returnLabel.toGoto())
        return UnitDomain.element
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext<ResultTrackingContext>): Exp =
        // We ignore the accumulator: we just want to get the result of the last expression.
        block.statements.fold<FirStatement, Exp>(UnitDomain.element) { _, it -> data.convert(it) }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: StmtConversionContext<ResultTrackingContext>): Exp =
        when (constExpression.kind) {
            ConstantValueKind.Int -> Exp.IntLit((constExpression.value as Long).toInt())
            ConstantValueKind.Boolean -> Exp.BoolLit(constExpression.value as Boolean)
            ConstantValueKind.Null -> (data.embedType(constExpression) as NullableTypeEmbedding).nullVal
            else -> handleUnimplementedElement("Constant Expression of type ${constExpression.kind} is not yet implemented.", data)
        }

    override fun visitWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): Exp =
        // TODO: find a way to not evaluate subject multiple times if it is a function call
        data.convert(whenSubjectExpression.whenRef.value.subject!!)

    private fun convertWhenBranches(whenBranches: Iterator<FirWhenBranch>, data: StmtConversionContext<ResultTrackingContext>) {
        // NOTE: I think that this will also work with "in" or "is" conditions when implemented, but I'm not 100% sure
        if (!whenBranches.hasNext()) return

        val branch = whenBranches.next()

        // Note that only the last condition can be a FirElseIfTrue
        if (branch.condition is FirElseIfTrueCondition) {
            data.convertAndCapture(branch.result)
        } else {
            val cond = data.convert(branch.condition)
            val thenCtx = data.newBlock()
            thenCtx.convertAndCapture(branch.result)
            val elseCtx = data.newBlock()
            convertWhenBranches(whenBranches, elseCtx)
            data.addStatement(Stmt.If(cond, thenCtx.block, elseCtx.block))
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: StmtConversionContext<ResultTrackingContext>): Exp {
        val ctx = if (whenExpression.usedAsExpression) {
            data.withResult(data.embedType(whenExpression))
        } else {
            data
        }
        convertWhenBranches(whenExpression.branches.iterator(), ctx)

        return ctx.resultExp
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): Exp {
        val type = data.embedType(propertyAccessExpression)
        return when (val symbol = propertyAccessExpression.calleeSymbol) {
            is FirValueParameterSymbol -> data.getVariableEmbedding(symbol.callableId.embedName(), type).toLocalVar()
            is FirPropertySymbol -> {
                val varEmbedding = data.getVariableEmbedding(symbol.callableId.embedName(), type)
                if (symbol.isLocal) {
                    return varEmbedding.toLocalVar()
                } else {
                    val receiver = data.convert(propertyAccessExpression.dispatchReceiver)
                    val fieldAccess = Exp.FieldAccess(receiver, varEmbedding.toField())
                    val accPred = AccessPredicate.FieldAccessPredicate(fieldAccess, PermExp.FullPerm())
                    return data.withResult(varEmbedding.type) {
                        // We do not track permissions over time and thus have to inhale and exhale the permission when reading a field.
                        data.addStatement(Stmt.Inhale(accPred))
                        data.addStatement(Stmt.assign(resultExp, fieldAccess))
                        data.addStatement(Stmt.Exhale(accPred))
                    }
                }
            }
            else -> handleUnimplementedElement("Property access ${propertyAccessExpression.source} not implemented", data)
        }
    }

    override fun visitEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: StmtConversionContext<ResultTrackingContext>,
    ): Exp {
        if (equalityOperatorCall.arguments.size != 2) {
            throw IllegalArgumentException("Invalid equality comparison $equalityOperatorCall, can only compare 2 elements.")
        }
        val left = data.convert(equalityOperatorCall.arguments[0])
        val right = data.convert(equalityOperatorCall.arguments[1])

        val leftType = data.embedType(equalityOperatorCall.arguments[0])
        val rightType = data.embedType(equalityOperatorCall.arguments[1])

        return when (equalityOperatorCall.operation) {
            FirOperation.EQ -> convertEqCmp(left, leftType, right, rightType)
            FirOperation.NOT_EQ -> Exp.Not(convertEqCmp(left, leftType, right, rightType))
            else -> handleUnimplementedElement("Equality comparison operation ${equalityOperatorCall.operation} not yet implemented.", data)
        }
    }

    private fun convertEqCmp(left: Exp, leftType: TypeEmbedding, right: Exp, rightType: TypeEmbedding): Exp =
        if (leftType is NullableTypeEmbedding && rightType !is NullableTypeEmbedding) {
            Exp.And(
                Exp.NeCmp(left, leftType.nullVal),
                // TODO: Replace the Eq comparison with a member call function to `left.equals`
                Exp.EqCmp(left.convertType(leftType, leftType.elementType), right.convertType(rightType, leftType.elementType))
            )
        } else if (leftType is NullableTypeEmbedding && rightType is NullableTypeEmbedding) {
            Exp.Or(
                Exp.And(
                    Exp.EqCmp(left, leftType.nullVal),
                    Exp.EqCmp(right, rightType.nullVal),
                ),
                // TODO: Replace the Eq comparison with a member call function to `left.equals`
                Exp.And(
                    Exp.And(
                        Exp.NeCmp(left, leftType.nullVal),
                        Exp.NeCmp(right, rightType.nullVal),
                    ),
                    Exp.EqCmp(left.convertType(leftType, leftType.elementType), right.convertType(rightType, leftType.elementType))
                )
            )
        } else {
            // TODO: Replace the Eq comparison with a member call function to `left.equals`
            Exp.EqCmp(left, right.convertType(rightType, leftType))
        }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext<ResultTrackingContext>): Exp {
        val symbol = functionCall.calleeCallableSymbol
        val id = symbol.callableId
        val specialFunc = SpecialFunctions.byCallableId[id]
        val argsFir = getFunctionCallArguments(functionCall)
        if (specialFunc != null) {
            if (specialFunc !is SpecialFunctionImplementation) return UnitDomain.element
            return specialFunc.convertCall(argsFir.map(data::convert), data)
        }

        val isInline = functionCall.calleeCallableSymbol.resolvedStatus.isInline
        if (isInline) {
            return data.withResult(data.embedType(functionCall)) {
                processInlineFunctionCall(functionCall, this)
            }
        }

        val calleeSig = when (symbol) {
            is FirNamedFunctionSymbol -> data.embedFunction(symbol)
            is FirConstructorSymbol -> data.embedFunction(symbol)
            else -> TODO("Identify and handle other cases")
        }

        return data.withResult(calleeSig.returnType) {
            val args = argsFir
                .zip(calleeSig.formalArgs)
                .map { (arg, formalArg) -> data.convert(arg).convertType(data.embedType(arg), formalArg.type) }

            data.addStatement(calleeSig.toMethodCall(args, this.resultCtx.resultVar))
        }
    }

    @OptIn(SymbolInternals::class)
    private fun processInlineFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext<VarResultTrackingContext>) {
        val symbol = functionCall.calleeNamedFunctionSymbol
        val signature = data.embedFunction(symbol)
        val inlineBody = symbol.fir.body ?: throw Exception("Function symbol $symbol has a null body")
        val ctx = data.newBlock()
        val inlineArgs: List<MangledName> = symbol.valueParameterSymbols.map { it.embedName() }
        val callArgs = getFunctionCallArguments(functionCall).map { ctx.convertAndStore(it).name }
        val substitutionParams = inlineArgs.zip(callArgs).toMap()

        val inlineCtx = ctx.withInlineContext(signature, ctx.resultCtx.resultVar, substitutionParams)
        inlineCtx.convert(inlineBody)
        // TODO: add these labels automatically.
        inlineCtx.addDeclaration(inlineCtx.returnLabel.toDecl())
        inlineCtx.addStatement(inlineCtx.returnLabel.toStmt())
        // Note: Putting the block inside the then branch of an if-true statement is a little a hack to make Viper respect the scoping
        data.addStatement(Stmt.If(Exp.BoolLit(true), inlineCtx.block, Stmt.Seqn(listOf(), listOf())))
    }

    override fun visitImplicitInvokeCall(
        implicitInvokeCall: FirImplicitInvokeCall,
        data: StmtConversionContext<ResultTrackingContext>,
    ): Exp {
        val args = getFunctionCallArguments(implicitInvokeCall).map(data::convert)
        val retType = implicitInvokeCall.calleeCallableSymbol.resolvedReturnType
        return data.withResult(data.embedType(retType)) {
            // NOTE: Since it is only relevant to update the number of times that a function object is called,
            // the function call invocation is intentionally not assigned to the return variable
            data.addStatement(InvokeFunctionObjectMethod.toMethodCall(args.take(1), listOf()))
        }
    }

    private fun getFunctionCallArguments(functionCall: FirFunctionCall): List<FirExpression> {
        val receiver = if (functionCall.dispatchReceiver !is FirNoReceiverExpression) {
            listOf(functionCall.dispatchReceiver)
        } else {
            emptyList()
        }
        return receiver + functionCall.argumentList.arguments
    }

    override fun visitProperty(property: FirProperty, data: StmtConversionContext<ResultTrackingContext>): Exp {
        val symbol = property.symbol
        val type = property.returnTypeRef.coneTypeOrNull!!
        if (!symbol.isLocal) {
            throw Exception("StmtConversionVisitor should not encounter non-local properties.")
        }
        val cvar = data.getVariableEmbedding(symbol.callableId.embedName(), data.embedType(type))
        data.addDeclaration(cvar.toLocalVarDecl())
        property.initializer?.let {
            val initializerExp = data.convert(it)
            val initializerType = data.embedType(it)
            data.addStatement(Stmt.LocalVarAssign(cvar.toLocalVar(), initializerExp.convertType(initializerType, cvar.type)))
        }
        return UnitDomain.element
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: StmtConversionContext<ResultTrackingContext>): Exp {
        val condCtx = data.withResult(BooleanTypeEmbedding)
        condCtx.convertAndCapture(whileLoop.condition)

        val bodyCtx = condCtx.newBlock()
        bodyCtx.convert(whileLoop.block)
        bodyCtx.convertAndCapture(whileLoop.condition)

        data.addStatement(Stmt.While(condCtx.resultExp, invariants = data.postconditions, bodyCtx.block))
        return UnitDomain.element
    }

    override fun visitVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: StmtConversionContext<ResultTrackingContext>,
    ): Exp {
        // It is not entirely clear whether we can get away with ignoring the distinction between
        // lvalues and rvalues, but let's try to at first, and we'll fix it later if it turns out
        // not to work.
        val convertedLValue = data.convert(variableAssignment.lValue)
        val lValueType = data.embedType(variableAssignment.lValue)
        val convertedRValue = data.convert(variableAssignment.rValue)
        val rValueType = data.embedType(variableAssignment.rValue)
        data.addStatement(Stmt.assign(convertedLValue, convertedRValue.convertType(rValueType, lValueType)))
        return UnitDomain.element
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): Exp {
        val exp = data.convert(smartCastExpression.originalExpression)
        val expType = data.embedType(smartCastExpression.originalExpression)
        val newType = data.embedType(smartCastExpression.smartcastType.coneType)
        return exp.convertType(expType, newType)
    }

    override fun visitBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): Exp {
        val left = data.convert(binaryLogicExpression.leftOperand)
        return data.withResult(BooleanTypeEmbedding) {
            val rightCtx = newBlock()
            rightCtx.convertAndCapture(binaryLogicExpression.rightOperand)
            when (binaryLogicExpression.kind) {
                LogicOperationKind.AND -> {
                    val constCtx = newBlock()
                    constCtx.capture(Exp.BoolLit(false), BooleanTypeEmbedding)
                    data.addStatement(Stmt.If(left, rightCtx.block, constCtx.block))
                }
                LogicOperationKind.OR -> {
                    val constCtx = newBlock()
                    constCtx.capture(Exp.BoolLit(true), BooleanTypeEmbedding)
                    data.addStatement(Stmt.If(left, constCtx.block, rightCtx.block))
                }
            }
        }
    }

    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): Exp =
        data.signature.receiver?.toLocalVar()
            ?: throw IllegalArgumentException("Can't resolve the 'this' receiver since the function does not have one.")

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: StmtConversionContext<ResultTrackingContext>): Exp {
        val argument = data.convert(typeOperatorCall.arguments[0])
        val conversionType = data.embedType(typeOperatorCall.conversionTypeRef.coneType)
        return when (typeOperatorCall.operation) {
            FirOperation.IS -> TypeDomain.isSubtype(TypeOfDomain.typeOf(argument), conversionType.kotlinType)
            FirOperation.NOT_IS -> Exp.Not(TypeDomain.isSubtype(TypeOfDomain.typeOf(argument), conversionType.kotlinType))
            else -> handleUnimplementedElement("Can't embed type operator ${typeOperatorCall.operation}.", data)
        }
    }

    private val FirResolvable.calleeSymbol: FirBasedSymbol<*>
        get() = calleeReference.toResolvedBaseSymbol()!!
    private val FirResolvable.calleeCallableSymbol: FirCallableSymbol<*>
        get() = calleeReference.toResolvedCallableSymbol()!!
    private val FirResolvable.calleeNamedFunctionSymbol: FirNamedFunctionSymbol
        get() = calleeReference.toResolvedNamedFunctionSymbol()!!

    private fun handleUnimplementedElement(msg: String, data: StmtConversionContext<ResultTrackingContext>): Exp =
        when (data.config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                TODO(msg)
            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                System.err.println(msg) // hack for while we're actively developing this to see what we're missing
                data.addStatement(Stmt.Assume(Exp.BoolLit(false)))
                UnitDomain.element
            }
        }
}