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
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.*
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.*
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.Exp
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
object StmtConversionVisitor : FirVisitor<ExpEmbedding, StmtConversionContext<ResultTrackingContext>>() {
    // Note that in some cases we don't expect to ever implement it: we are only
    // translating statements here, after all.  It isn't 100% clear how best to
    // communicate this.
    override fun visitElement(element: FirElement, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        handleUnimplementedElement("Not yet implemented for $element (${element.source.text})", data)

    override fun visitReturnExpression(
        returnExpression: FirReturnExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val expr = data.convert(returnExpression.result)
        // returnTarget is null when it is the implicit return of a lambda
        val returnTargetName = returnExpression.target.labelName
        val (retVar, retLabel) = data.resolveReturnTarget(returnTargetName)
        retVar.setValue(expr, data, returnExpression.source)
        data.addStatement(retLabel.toGoto(returnExpression.source.asPosition))
        return UnitLit
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        // We ignore the accumulator: we just want to get the result of the last expression.
        block.statements.fold<_, ExpEmbedding>(UnitLit) { _, it -> data.convert(it) }

    override fun <T> visitLiteralExpression(
        constExpression: FirLiteralExpression<T>,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding =
        when (constExpression.kind) {
            ConstantValueKind.Int -> IntLit((constExpression.value as Long).toInt())
            ConstantValueKind.Boolean -> BooleanLit(constExpression.value as Boolean)
            ConstantValueKind.Null -> (data.embedType(constExpression) as NullableTypeEmbedding).nullVal
            else -> handleUnimplementedElement("Constant Expression of type ${constExpression.kind} is not yet implemented.", data)
        }

    override fun visitWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding = data.whenSubject!!

    private fun convertWhenBranches(whenBranches: Iterator<FirWhenBranch>, data: StmtConversionContext<ResultTrackingContext>) {
        if (!whenBranches.hasNext()) return

        val branch = whenBranches.next()

        // Note that only the last condition can be a FirElseIfTrue
        if (branch.condition is FirElseIfTrueCondition) {
            data.convertAndCapture(branch.result)
        } else {
            val cond = data.convert(branch.condition)
            val thenBlock = data.withNewScopeToBlock {
                convertAndCapture(branch.result)
            }
            val elseBlock = data.withNewScopeToBlock {
                convertWhenBranches(whenBranches, this)
            }
            data.addStatement(Stmt.If(cond.pureToViper(), thenBlock, elseBlock, branch.condition.source.asPosition))
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        val type = data.embedType(whenExpression)
        val ctx = if (type != NothingTypeEmbedding && type != UnitTypeEmbedding) {
            data.addResult(type)
        } else {
            data.removeResult()
        }
        return ctx.withNewScope {
            val subj: VariableEmbedding? = whenExpression.subject?.let {
                val subjExp = convert(it)
                when (val firSubjVar = whenExpression.subjectVariable) {
                    null -> store(subjExp)
                    else -> declareLocal(firSubjVar.name, subjExp.type, subjExp)
                }
            }
            withWhenSubject(subj) {
                convertWhenBranches(whenExpression.branches.iterator(), this)
            }
            resultExp
        }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val propertyAccess = data.embedPropertyAccess(propertyAccessExpression)
        return propertyAccess.getValue(data, propertyAccessExpression.source)
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
                EqCmp(left.withType(leftType.elementType), right.withType(leftType.elementType)),
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
                    EqCmp(left.withType(leftType.elementType), right.withType(leftType.elementType)),
                ),
            )
        } else {
            // TODO: Replace the Eq comparison with a member call function to `left.equals`
            EqCmp(left, right.withType(leftType))
        }
    }

    override fun visitComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val dispatchReceiver = comparisonExpression.compareToCall.dispatchReceiver
        val arg = comparisonExpression.compareToCall.argumentList.arguments.firstOrNull()
        if (dispatchReceiver == null) {
            throw IllegalArgumentException("found compareTo call with null receiver")
        }
        if (arg == null) {
            throw IllegalArgumentException("found compareTo call with null argument at position 0")
        }
        val left = data.convert(dispatchReceiver)
        val right = data.convert(arg)
        return when (comparisonExpression.operation) {
            FirOperation.LT -> LtCmp(left, right)
            FirOperation.LT_EQ -> LeCmp(left, right)
            FirOperation.GT -> GtCmp(left, right)
            FirOperation.GT_EQ -> GeCmp(left, right)
            else -> throw IllegalArgumentException("expected comparison operation but found ${comparisonExpression.operation}")
        }
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        val symbol = functionCall.calleeCallableSymbol
        val callee = data.embedFunction(symbol as FirFunctionSymbol<*>)
        return callee.insertCall(functionCall.functionCallArguments.map(data::convert), data, functionCall.source)
    }

    override fun visitImplicitInvokeCall(
        implicitInvokeCall: FirImplicitInvokeCall,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val receiverSymbol = implicitInvokeCall.dispatchReceiver?.calleeSymbol
            ?: throw NotImplementedError("Implicit invoke calls only support a limited range of receivers at the moment.")
        val args = implicitInvokeCall.argumentList.arguments.map(data::convert)
        return when (val exp = data.embedLocalSymbol(receiverSymbol).ignoringMetaNodes()) {
            is LambdaExp -> {
                // The lambda is already the receiver, so we do not need to convert it.
                // TODO: do this more uniformly: convert the receiver, see it is a lambda, use insertCall on it.
                exp.insertCall(args, data, implicitInvokeCall.source)
            }
            else -> {
                val retType = implicitInvokeCall.calleeCallableSymbol.resolvedReturnType
                args.filter { it.type is UnspecifiedFunctionTypeEmbedding }
                    .forEach {
                        val leakFunctionObjectCall = Stmt.Assert(
                            DuplicableFunction.toFuncApp(
                                listOf(it.pureToViper()),
                                implicitInvokeCall.source.asPosition
                            )
                        )
                        data.addStatement(leakFunctionObjectCall)
                    }
                data.withResult(data.embedType(retType)) {
                    // NOTE: Since it is only relevant to update the number of times that a function object is called,
                    // the function call invocation is intentionally not assigned to the return variable
                    val receiver = listOfNotNull(implicitInvokeCall.dispatchReceiver).map(data::convert)
                    data.addStatement(
                        InvokeFunctionObjectMethod.toMethodCall(
                            receiver.pureToViper(),
                            listOf(),
                            implicitInvokeCall.source.asPosition
                        )
                    )
                }
            }
        }
    }

    override fun visitProperty(property: FirProperty, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        val symbol = property.symbol
        if (!symbol.isLocal) {
            throw IllegalStateException("StmtConversionVisitor should not encounter non-local properties.")
        }
        data.declareLocal(
            symbol.name,
            data.embedType(symbol.resolvedReturnType),
            property.initializer?.let { data.convert(it) },
            symbol.source
        )
        return UnitLit
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        data.withFreshWhile {
            val condCtx = addResult(BooleanTypeEmbedding)
            condCtx.convertAndCapture(whileLoop.condition)
            val bodyBlock = condCtx.withNewScopeToBlock {
                whileLoop.label?.name?.let { addLoopName(it) }
                convert(whileLoop.block)
                convertAndCapture(whileLoop.condition)
            }
            val postconditions = when (val sig = data.signature) {
                is FullNamedFunctionSignature -> sig.postconditions
                else -> listOf()
            }
            data.addStatement(
                Stmt.While(
                    condCtx.resultExp.pureToViper(),
                    invariants = postconditions.pureToViper(),
                    bodyBlock,
                    whileLoop.source.asPosition
                )
            )
        }
        return UnitLit
    }

    override fun visitBreakExpression(
        breakExpression: FirBreakExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val targetName = breakExpression.target.labelName
        val breakLabel = data.breakLabel(targetName)
        data.addStatement(breakLabel.toGoto(breakExpression.source.asPosition))
        return UnitLit
    }

    override fun visitContinueExpression(
        continueExpression: FirContinueExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val targetName = continueExpression.target.labelName
        val continueLabel = data.continueLabel(targetName)
        data.addStatement(continueLabel.toGoto(continueExpression.source.asPosition))
        return UnitLit
    }

    override fun visitVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val propertyAccess = variableAssignment.lValue as? FirPropertyAccessExpression
            ?: throw IllegalArgumentException("Left hand of an assignment must be a property access.")
        val embedding = data.embedPropertyAccess(propertyAccess)
        val convertedRValue = data.convert(variableAssignment.rValue)
        embedding.setValue(convertedRValue, data, variableAssignment.source)
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
            val rightBlock = withNewScopeToBlock {
                convertAndCapture(binaryLogicExpression.rightOperand)
            }
            when (binaryLogicExpression.kind) {
                LogicOperationKind.AND -> {
                    val constBlock = withNewScopeToBlock { capture(BooleanLit(false)) }
                    data.addStatement(Stmt.If(left.pureToViper(), rightBlock, constBlock, binaryLogicExpression.source.asPosition))
                }
                LogicOperationKind.OR -> {
                    val constBlock = withNewScopeToBlock { capture(BooleanLit(true)) }
                    data.addStatement(Stmt.If(left.pureToViper(), constBlock, rightBlock, binaryLogicExpression.source.asPosition))
                }
            }
        }
    }

    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding =
        data.signature.receiver
            ?: throw IllegalArgumentException("Can't resolve the 'this' receiver since the function does not have one.")

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val argument = data.convert(typeOperatorCall.arguments[0])
        val conversionType = data.embedType(typeOperatorCall.conversionTypeRef.coneType)
        return when (typeOperatorCall.operation) {
            FirOperation.IS -> Is(argument, conversionType)
            FirOperation.NOT_IS -> Not(Is(argument, conversionType))
            FirOperation.AS -> data.withResult(conversionType) {
                // TODO: this may require special handling of access permissions; we should look into a more systematic solution.
                resultCtx.resultVar.setValue(argument, this, typeOperatorCall.source)
                for (invariant in resultCtx.resultVar.accessInvariants()) {
                    addStatement(Stmt.Inhale(invariant.pureToViper()))
                }
            }
            FirOperation.SAFE_AS -> data.withResult(conversionType.getNullable()) {
                val ifBlock = withNewScopeToBlock {
                    resultCtx.resultVar.setValue(argument, this, null)
                    // If the argument was of a non-nullable type, then in this branch we know that the result is also non-null
                    // and can thus inhale stronger permissions.
                    val invariants = if (argument.type.isNullable) {
                        resultCtx.resultVar.accessInvariants()
                    } else {
                        conversionType.accessInvariants().fillHoles(Cast(resultExp, conversionType))
                    }

                    for (invariant in invariants) {
                        addStatement(Stmt.Inhale(invariant.pureToViper()))
                    }
                }
                val elseBlock = withNewScopeToBlock {
                    resultCtx.resultVar.setValue(NullLit(conversionType), this, null)
                }
                addStatement(Stmt.If(Is(argument, conversionType).pureToViper(), ifBlock, elseBlock, typeOperatorCall.source.asPosition))
            }
            else -> handleUnimplementedElement("Can't embed type operator ${typeOperatorCall.operation}.", data)
        }
    }

    override fun visitLambdaArgumentExpression(
        lambdaArgumentExpression: FirLambdaArgumentExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        // TODO: check whether there are other cases.
        val function = (lambdaArgumentExpression.expression as FirAnonymousFunctionExpression).anonymousFunction
        return LambdaExp(data.embedFunctionSignature(function.symbol), function, data)
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        val catchData = data.withCatches(tryExpression.catches) { catchData ->
            withNewScope {
                val maybeJumpToSomeCatch = {
                    for (catchBlock in catchData.blocks) {
                        nonDeterministically {
                            addStatement(catchBlock.entryLabel.toGoto(catchBlock.firCatch.source.asPosition))
                        }
                    }
                }
                maybeJumpToSomeCatch()
                convert(tryExpression.tryBlock)
                maybeJumpToSomeCatch()
                addStatement(catchData.exitLabel.toGoto())
            }
        }
        for (catchBlock in catchData.blocks) {
            data.withNewScope {
                addStatement(catchBlock.entryLabel.toStmt(catchBlock.firCatch.source.asPosition))
                registerLocalPropertyName(catchBlock.firCatch.parameter.name)
                convert(catchBlock.firCatch.block)
                addStatement(catchData.exitLabel.toGoto(catchBlock.firCatch.source.asPosition))
            }
        }
        data.addStatement(catchData.exitLabel.toStmt(tryExpression.source.asPosition))
        return UnitLit
    }

    override fun visitElvisExpression(
        elvisExpression: FirElvisExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val lhs = data.convert(elvisExpression.lhs)
        val expType = data.embedType(elvisExpression.resolvedType)
        return data.withResult(expType) {
            val elseBlock = withNewScopeToBlock {
                convertAndCapture(elvisExpression.rhs)
            }
            val nonNullBranch = withNewScopeToBlock {
                capture(lhs.withType(expType))
            }
            addStatement(
                Stmt.If(
                    EqCmp(lhs, (lhs.type as NullableTypeEmbedding).nullVal).pureToViper(),
                    elseBlock,
                    nonNullBranch,
                    elvisExpression.source.asPosition
                )
            )
        }
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding {
        val selector = safeCallExpression.selector
        val receiver = data.convert(safeCallExpression.receiver)

        val checkedSafeCallSubjectType = data.embedType(safeCallExpression.checkedSubjectRef.value.resolvedType)

        return if (selector is FirExpression) {
            val expType = data.embedType(safeCallExpression.resolvedType)
            data.withResult(expType) {
                val whenNotNullCall = withNewScopeToBlock {
                    withCheckedSafeCallSubject(receiver.withType(checkedSafeCallSubjectType)) {
                        convertAndCapture(selector)
                    }
                }

                val whenNull = withNewScopeToBlock {
                    capture((expType as NullableTypeEmbedding).nullVal)
                }
                addStatement(
                    Stmt.If(
                        EqCmp(receiver, (receiver.type as NullableTypeEmbedding).nullVal).pureToViper(),
                        whenNull,
                        whenNotNullCall,
                        safeCallExpression.source.asPosition
                    )
                )
            }
        } else {
            val whenNotNullCall = data.withNewScopeToBlock {
                withCheckedSafeCallSubject(receiver.withType(checkedSafeCallSubjectType)) {
                    convert(selector)
                }
            }
            data.addStatement(
                Stmt.If(
                    NeCmp(receiver, (receiver.type as NullableTypeEmbedding).nullVal).pureToViper(),
                    whenNotNullCall,
                    Stmt.Seqn(),
                    safeCallExpression.source.asPosition
                )
            )
            UnitLit
        }
    }

    override fun visitCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: StmtConversionContext<ResultTrackingContext>,
    ): ExpEmbedding = data.checkedSafeCallSubject
        ?: throw IllegalArgumentException("Trying to resolve checked subject $checkedSafeCallSubject which was not captured in StmtConversionContext")

    private fun handleUnimplementedElement(msg: String, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        when (data.config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                TODO(msg)
            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                data.errorCollector.addMinorError(msg)
                data.addStatement(Stmt.Inhale(Exp.BoolLit(false)))
                UnitLit
            }
        }
}

object StmtConversionVisitorExceptionWrapper : FirVisitor<ExpEmbedding, StmtConversionContext<ResultTrackingContext>>() {
    override fun visitElement(element: FirElement, data: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
        try {
            return element.accept(StmtConversionVisitor, data)
        } catch (e: Exception) {
            data.errorCollector.addErrorInfo("... while converting ${element.source.text}")
            throw e
        }
    }
}
