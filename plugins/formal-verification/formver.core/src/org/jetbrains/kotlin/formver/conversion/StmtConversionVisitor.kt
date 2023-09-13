/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.calleeCallableSymbol
import org.jetbrains.kotlin.formver.calleeSymbol
import org.jetbrains.kotlin.formver.domains.TypeDomain
import org.jetbrains.kotlin.formver.domains.TypeOfDomain
import org.jetbrains.kotlin.formver.domains.UnitDomain
import org.jetbrains.kotlin.formver.domains.convertType
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.functionCallArguments
import org.jetbrains.kotlin.formver.viper.ast.AccessPredicate
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.viper.MangledName
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
object StmtConversionVisitor : FirVisitor<ExpEmbedding, StmtConversionContext<ResultTrackingContext>>() {
    // Note that in some cases we don't expect to ever implement it: we are only
    // translating statements here, after all.  It isn't 100% clear how best to
    // communicate this.
    override fun visitElement(element: FirElement, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        handleUnimplementedElement("Not yet implemented for $element (${element.source.text})", data)

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        val expr = data.convert(returnExpression.result)
        data.addStatement(Stmt.LocalVarAssign(data.returnVar.toViper(), expr.withType(data.returnVar.type).toViper()))
        data.addStatement(data.returnLabel.toGoto())
        return UnitLit
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        // We ignore the accumulator: we just want to get the result of the last expression.
        block.statements.fold<_, ExpEmbedding>(UnitLit) { _, it -> data.convert(it) }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        when (constExpression.kind) {
            ConstantValueKind.Int -> IntLit((constExpression.value as Long).toInt())
            ConstantValueKind.Boolean -> BooleanLit(constExpression.value as Boolean)
            ConstantValueKind.Null -> (data.embedType(constExpression) as NullableTypeEmbedding).nullVal
            else -> handleUnimplementedElement("Constant Expression of type ${constExpression.kind} is not yet implemented.", data)
        }

    override fun visitWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding =
        // TODO: find a way to not evaluate subject multiple times if it is a function call
        data.convert(whenSubjectExpression.whenRef.value.subject!!)

    private fun convertWhenBranches(whenBranches: Iterator<FirWhenBranch>, data: StmtConversionContext<ResultTrackingContext>) {
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
            data.addStatement(Stmt.If(cond.toViper(), thenCtx.block, elseCtx.block))
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
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
    ): ExpEmbedding {
        val type = data.embedType(propertyAccessExpression)
        return when (val symbol = propertyAccessExpression.calleeSymbol) {
            is FirValueParameterSymbol -> data.getVariableEmbedding(symbol.callableId.embedName(), type)
            is FirPropertySymbol -> {
                val varEmbedding = data.getVariableEmbedding(symbol.callableId.embedName(), type)
                if (symbol.isLocal) {
                    return varEmbedding
                }

                val receiver = data.convert(propertyAccessExpression.dispatchReceiver!!)

                return when (val getter = symbol.getter) {
                    is FirDefaultPropertyGetter -> {
                        val fieldAccess = Exp.FieldAccess(receiver.toViper(), varEmbedding.toField())
                        val accPred = AccessPredicate.FieldAccessPredicate(fieldAccess, PermExp.FullPerm())

                        data.withResult(varEmbedding.type) {
                            // We do not track permissions over time and thus have to inhale and exhale the permission when reading a field.
                            data.addStatement(Stmt.Inhale(accPred))
                            data.addStatement(Stmt.assign(resultExp.toViper(), fieldAccess))
                            resultCtx.resultVar.provenInvariants().forEach {
                                data.addStatement(Stmt.Inhale(it))
                            }
                            data.addStatement(Stmt.Exhale(accPred))
                        }
                    }
                    else -> {
                        val method = data.embedFunction(getter.symbol)
                        data.withResult(varEmbedding.type) {
                            val methodCall = method.toMethodCall(listOf(receiver.toViper()), resultCtx.resultVar)
                            data.addStatement(methodCall)
                        }
                    }
                }
            }
            else -> handleUnimplementedElement("Property access ${propertyAccessExpression.source} not implemented", data)
        }
    }

    override fun visitEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        if (equalityOperatorCall.arguments.size != 2) {
            throw IllegalArgumentException("Invalid equality comparison $equalityOperatorCall, can only compare 2 elements.")
        }
        val left = data.convert(equalityOperatorCall.arguments[0])
        val right = data.convert(equalityOperatorCall.arguments[1])

        return when (equalityOperatorCall.operation) {
            FirOperation.EQ -> convertEqCmp(left, right)
            FirOperation.NOT_EQ -> Not(convertEqCmp(left, right))
            else -> handleUnimplementedElement("Equality comparison operation ${equalityOperatorCall.operation} not yet implemented.", data)
        }
    }

    private fun convertEqCmp(left: ExpEmbedding, right: ExpEmbedding): ExpEmbedding {
        val leftType = left.type
        val rightType = right.type
        return if (leftType is NullableTypeEmbedding && rightType !is NullableTypeEmbedding) {
            And(
                NeCmp(left, leftType.nullVal),
                // TODO: Replace the Eq comparison with a member call function to `left.equals`
                EqCmp(left.withType(leftType.elementType), right.withType(leftType.elementType))
            )
        } else if (leftType is NullableTypeEmbedding && rightType is NullableTypeEmbedding) {
            Or(
                And(
                    EqCmp(left, leftType.nullVal),
                    EqCmp(right, rightType.nullVal),
                ),
                // TODO: Replace the Eq comparison with a member call function to `left.equals`
                And(
                    And(
                        NeCmp(left, leftType.nullVal),
                        NeCmp(right, rightType.nullVal),
                    ),
                    EqCmp(left.withType(leftType.elementType), right.withType(leftType.elementType))
                )
            )
        } else {
            // TODO: Replace the Eq comparison with a member call function to `left.equals`
            EqCmp(left, right.withType(leftType))
        }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        val symbol = functionCall.calleeCallableSymbol
        val id = symbol.callableId
        val specialFunc = SpecialKotlinFunctions.byCallableId[id]
        val argsFir = functionCall.functionCallArguments
        if (specialFunc != null) {
            if (specialFunc !is SpecialKotlinFunctionImplementation) return UnitLit
            return specialFunc.convertCall(argsFir.map(data::convert), data)
        }

        val callee = data.embedFunction(symbol as FirFunctionSymbol<*>)
        return callee.insertCall(argsFir, data)
    }

    override fun visitImplicitInvokeCall(
        implicitInvokeCall: FirImplicitInvokeCall,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val args = implicitInvokeCall.functionCallArguments.map(data::convert)
        val retType = implicitInvokeCall.calleeCallableSymbol.resolvedReturnType
        val calleeName = LocalName(implicitInvokeCall.calleeReference.name)
        val lambda = data.getLambdaOrNull(calleeName)
        if (lambda != null) {
            return data.withResult(data.embedType(retType)) {
                // NOTE: it is not needed to make distinction between implicit or explicit parameters
                val lambdaArgs = lambda.lambdaArgs()
                val callArgs = data.method.getFunctionCallSubstitutionItems(implicitInvokeCall.argumentList.arguments, data)
                val subs: Map<MangledName, SubstitutionItem> = lambdaArgs.zip(callArgs).toMap()
                val lambdaCtx = this.newBlock().withInlineContext(this.method, this.resultCtx.resultVar, subs)
                lambdaCtx.convert(lambda.lambdaBody())
                // NOTE: It is necessary to drop the last stmt because is a wrong goto
                val sqn = lambdaCtx.block.copy(stmts = lambdaCtx.block.stmts.dropLast(1))
                sqn.scopedStmtsDeclaration.forEach(data::addDeclaration)
                sqn.stmts.forEach(data::addStatement)
            }
        }

        return data.withResult(data.embedType(retType)) {
            // NOTE: Since it is only relevant to update the number of times that a function object is called,
            // the function call invocation is intentionally not assigned to the return variable
            data.addStatement(InvokeFunctionObjectMethod.toMethodCall(args.take(1).toViper(), listOf()))
        }
    }

    override fun visitProperty(property: FirProperty, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        val symbol = property.symbol
        val type = property.returnTypeRef.coneTypeOrNull!!
        if (!symbol.isLocal) {
            throw Exception("StmtConversionVisitor should not encounter non-local properties.")
        }
        val cvar = data.getVariableEmbedding(symbol.callableId.embedName(), data.embedType(type))
        data.addDeclaration(cvar.toLocalVarDecl())
        property.initializer?.let {
            val initializerExp = data.convert(it)
            data.addStatement(Stmt.LocalVarAssign(cvar.toViper(), initializerExp.withType(cvar.type).toViper()))
        }
        return UnitLit
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        data.inNewWhileBlock { newWhileContext ->
            val condCtx = newWhileContext.withResult(BooleanTypeEmbedding)
            val bodyCtx = condCtx.newBlock()
            condCtx.convertAndCapture(whileLoop.condition)
            bodyCtx.convert(whileLoop.block)
            bodyCtx.convertAndCapture(whileLoop.condition)
            data.addStatement(Stmt.While(condCtx.resultExp.toViper(), invariants = data.method.postconditions, bodyCtx.block))
        }
        return UnitLit
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        data.addStatement(data.breakLabel.toGoto())
        return UnitLit
    }

    override fun visitContinueExpression(
        continueExpression: FirContinueExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        data.addStatement(data.continueLabel.toGoto())
        return UnitLit
    }

    override fun visitVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val lValueType = data.embedType(variableAssignment.lValue)
        val convertedRValue = data.convert(variableAssignment.rValue)

        if (variableAssignment.lValue.isClassPropertyAccess) {
            val lValue = variableAssignment.lValue as FirPropertyAccessExpression
            val lValueSymbol = lValue.calleeSymbol as FirPropertySymbol
            val receiver = data.convert(lValue.dispatchReceiver!!)

            when (val setter = lValueSymbol.setter) {
                is FirDefaultPropertySetter -> {
                    // No custom setters have been defined, we can assign the fields normally.
                    val varEmbedding = VariableEmbedding(lValueSymbol.callableId.embedName(), lValueType)
                    val fieldAccess = Exp.FieldAccess(receiver.toViper(), varEmbedding.toField())
                    val accPred = AccessPredicate.FieldAccessPredicate(fieldAccess, PermExp.FullPerm())
                    data.addStatement(Stmt.Inhale(accPred))
                    data.addStatement(Stmt.assign(fieldAccess, convertedRValue.withType(lValueType).toViper()))
                    data.addStatement(Stmt.Exhale(accPred))
                }
                else -> {
                    // Since a custom setter has been defined, we should generate a statement to invoke a method call
                    val method = data.embedFunction(setter.symbol)
                    data.withResult(UnitTypeEmbedding) {
                        data.addStatement(method.toMethodCall(listOf(receiver, convertedRValue).toViper(), this.resultCtx.resultVar))
                    }
                }
            }
        } else {
            val convertedLValue = data.convert(variableAssignment.lValue)
            data.addStatement(Stmt.assign(convertedLValue.toViper(), convertedRValue.withType(lValueType).toViper()))
        }

        return UnitLit
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val exp = data.convert(smartCastExpression.originalExpression)
        val newType = data.embedType(smartCastExpression.smartcastType.coneType)
        return exp.withType(newType)
    }

    override fun visitBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val left = data.convert(binaryLogicExpression.leftOperand)
        return data.withResult(BooleanTypeEmbedding) {
            val rightCtx = newBlock()
            rightCtx.convertAndCapture(binaryLogicExpression.rightOperand)
            when (binaryLogicExpression.kind) {
                LogicOperationKind.AND -> {
                    val constCtx = newBlock()
                    constCtx.capture(BooleanLit(false))
                    data.addStatement(Stmt.If(left.toViper(), rightCtx.block, constCtx.block))
                }
                LogicOperationKind.OR -> {
                    val constCtx = newBlock()
                    constCtx.capture(BooleanLit(true))
                    data.addStatement(Stmt.If(left.toViper(), constCtx.block, rightCtx.block))
                }
            }
        }
    }

    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding =
        data.method.receiver
            ?: throw IllegalArgumentException("Can't resolve the 'this' receiver since the function does not have one.")

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        val argument = data.convert(typeOperatorCall.arguments[0])
        val conversionType = data.embedType(typeOperatorCall.conversionTypeRef.coneType)
        return when (typeOperatorCall.operation) {
            FirOperation.IS -> Is(argument, conversionType)
            FirOperation.NOT_IS -> Not(Is(argument, conversionType))
            else -> handleUnimplementedElement("Can't embed type operator ${typeOperatorCall.operation}.", data)
        }
    }

    @OptIn(SymbolInternals::class)
    private val FirPropertySymbol.getter: FirPropertyAccessor
        get() = fir.getter!!
    @OptIn(SymbolInternals::class)
    private val FirPropertySymbol.setter: FirPropertyAccessor
        get() = fir.setter!!

    private val FirExpression.isClassPropertyAccess: Boolean
        get() = when (this) {
            is FirPropertyAccessExpression -> dispatchReceiver != null
            else -> false
        }

    private fun handleUnimplementedElement(msg: String, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        when (data.config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                TODO(msg)
            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                data.config.addMinorError(msg)
                data.addStatement(Stmt.Assume(Exp.BoolLit(false)))
                UnitLit
            }
        }
}

object StmtConversionVisitorExceptionWrapper : FirVisitor<ExpEmbedding, StmtConversionContext<ResultTrackingContext>>() {
    override fun visitElement(element: FirElement, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        try {
            return element.accept(StmtConversionVisitor, data)
        } catch (e: Throwable) {
            data.config.addErrorInfo("... while converting ${element.source.text}")
            throw e
        }
    }
}
