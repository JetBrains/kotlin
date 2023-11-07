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
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.calleeCallableSymbol
import org.jetbrains.kotlin.formver.calleeSymbol
import org.jetbrains.kotlin.formver.embeddings.BooleanTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.NullableTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.UnspecifiedFunctionTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.embeddings.callables.insertCall
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.functionCallArguments
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
object StmtConversionVisitor : FirVisitor<ExpEmbedding, StmtConversionContext>() {
    // Note that in some cases we don't expect to ever implement it: we are only
    // translating statements here, after all.  It isn't 100% clear how best to
    // communicate this.
    override fun visitElement(element: FirElement, data: StmtConversionContext): ExpEmbedding =
        handleUnimplementedElement("Not yet implemented for $element (${element.source.text})", data)

    override fun visitReturnExpression(
        returnExpression: FirReturnExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val expr = data.convert(returnExpression.result)
        // returnTarget is null when it is the implicit return of a lambda
        val returnTargetName = returnExpression.target.labelName
        val target = data.resolveReturnTarget(returnTargetName)
        return Block(Assign(target.variable, expr), Goto(target.label))
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext): ExpEmbedding =
        Block(block.statements.map(data::convert))

    override fun <T> visitLiteralExpression(
        constExpression: FirLiteralExpression<T>,
        data: StmtConversionContext,
    ): ExpEmbedding =
        when (constExpression.kind) {
            ConstantValueKind.Int -> IntLit((constExpression.value as Long).toInt())
            ConstantValueKind.Boolean -> BooleanLit(constExpression.value as Boolean)
            ConstantValueKind.Null -> data.embedType(constExpression).getNullable().nullVal
            else -> handleUnimplementedElement("Constant Expression of type ${constExpression.kind} is not yet implemented.", data)
        }

    override fun visitWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: StmtConversionContext,
    ): ExpEmbedding = data.whenSubject!!

    private fun convertWhenBranches(
        whenBranches: Iterator<FirWhenBranch>,
        type: TypeEmbedding,
        data: StmtConversionContext,
    ): ExpEmbedding {
        if (!whenBranches.hasNext()) return UnitLit

        val branch = whenBranches.next()

        // Note that only the last condition can be a FirElseIfTrue
        return if (branch.condition is FirElseIfTrueCondition) {
            data.withNewScope { convert(branch.result) }
        } else {
            val cond = data.convert(branch.condition)
            val thenExp = data.withNewScope { convert(branch.result) }
            val elseExp = convertWhenBranches(whenBranches, type, data)
            If(cond, thenExp, elseExp, type)
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: StmtConversionContext): ExpEmbedding =
        data.withNewScope {
            val type = data.embedType(whenExpression)
            val subj: Declare? = whenExpression.subject?.let {
                val subjExp = convert(it)
                when (val firSubjVar = whenExpression.subjectVariable) {
                    null -> declareAnonVar(subjExp.type, subjExp)
                    else -> declareLocalVariable(firSubjVar.symbol, subjExp)
                }
            }
            val body = withWhenSubject(subj?.variable) {
                convertWhenBranches(whenExpression.branches.iterator(), type, this)
            }
            subj?.let { Block(it, body) } ?: body
        }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val propertyAccess = data.embedPropertyAccess(propertyAccessExpression)
        return propertyAccess.getValue(data)
    }

    override fun visitEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        require(equalityOperatorCall.arguments.size == 2) {
            "Invalid equality comparison $equalityOperatorCall, can only compare 2 elements."
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
        // TODO: check whether isNullVal can be used here with no loss of generality.
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
        data: StmtConversionContext,
    ): ExpEmbedding {
        val dispatchReceiver = checkNotNull(comparisonExpression.compareToCall.dispatchReceiver) {
            "found 'compareTo' call with null receiver"
        }
        val arg = checkNotNull(comparisonExpression.compareToCall.argumentList.arguments.firstOrNull()) {
            "found `compareTo` call with no argument at position 0"
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

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext): ExpEmbedding {
        val symbol = functionCall.calleeCallableSymbol
        val callee = data.embedFunction(symbol as FirFunctionSymbol<*>)
        return callee.insertCall(functionCall.functionCallArguments.map(data::convert), data)
    }

    override fun visitImplicitInvokeCall(
        implicitInvokeCall: FirImplicitInvokeCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val receiver = implicitInvokeCall.dispatchReceiver
            ?: throw NotImplementedError("Implicit invoke calls only support a limited range of receivers at the moment.")
        val receiverSymbol = receiver.calleeSymbol
        val args = implicitInvokeCall.argumentList.arguments.map(data::convert)
        return when (val exp = data.embedLocalSymbol(receiverSymbol).ignoringMetaNodes()) {
            is LambdaExp -> {
                // The lambda is already the receiver, so we do not need to convert it.
                // TODO: do this more uniformly: convert the receiver, see it is a lambda, use insertCall on it.
                exp.insertCall(args, data)
            }
            else -> {
                val retType = data.embedType(implicitInvokeCall.calleeCallableSymbol.resolvedReturnType)
                val leaks = args.filter { it.type is UnspecifiedFunctionTypeEmbedding }
                    .map { Assert(DuplicableCall(it)) }
                val call = InvokeFunctionObject(data.convert(receiver), args, retType)
                Block(leaks + listOf(call))
            }
        }
    }

    override fun visitProperty(property: FirProperty, data: StmtConversionContext): ExpEmbedding {
        val symbol = property.symbol
        if (!symbol.isLocal) {
            throw IllegalStateException("StmtConversionVisitor should not encounter non-local properties.")
        }
        val type = data.embedType(symbol.resolvedReturnType)
        return data.declareLocalProperty(symbol, property.initializer?.let { data.convert(it).withType(type) })
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: StmtConversionContext): ExpEmbedding {
        val condition = data.convert(whileLoop.condition)
        val returnTarget = data.defaultResolvedReturnTarget
        val invariants = when (val sig = data.signature) {
            is FullNamedFunctionSignature -> sig.getPostconditions(returnTarget.variable)
            else -> listOf()
        }
        return data.withFreshWhile(whileLoop.label) {
            val body = convert(whileLoop.block)
            While(condition, body, breakLabel(), continueLabel(), invariants)
        }
    }

    override fun visitBreakExpression(
        breakExpression: FirBreakExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val targetName = breakExpression.target.labelName
        val breakLabel = data.breakLabel(targetName)
        return Goto(breakLabel)
    }

    override fun visitContinueExpression(
        continueExpression: FirContinueExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val targetName = continueExpression.target.labelName
        val continueLabel = data.continueLabel(targetName)
        return Goto(continueLabel)
    }

    override fun visitVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val propertyAccess = variableAssignment.lValue as? FirPropertyAccessExpression
            ?: throw IllegalArgumentException("Left hand of an assignment must be a property access.")
        val embedding = data.embedPropertyAccess(propertyAccess)
        val convertedRValue = data.convert(variableAssignment.rValue)
        return embedding.setValue(convertedRValue, data)
    }

    override fun visitSmartCastExpression(
        smartCastExpression: FirSmartCastExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val exp = data.convert(smartCastExpression.originalExpression)
        val newType = data.embedType(smartCastExpression.smartcastType.coneType)
        return exp.withType(newType)
    }

    override fun visitBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val left = data.convert(binaryLogicExpression.leftOperand)
        val right = data.convert(binaryLogicExpression.rightOperand)
        return when (binaryLogicExpression.kind) {
            LogicOperationKind.AND -> If(left, right, BooleanLit(false), BooleanTypeEmbedding)
            LogicOperationKind.OR -> If(left, BooleanLit(true), right, BooleanTypeEmbedding)
        }
    }

    override fun visitThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: StmtConversionContext,
    ): ExpEmbedding =
        data.signature.receiver
            ?: throw IllegalArgumentException("Can't resolve the 'this' receiver since the function does not have one.")

    override fun visitTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val argument = data.convert(typeOperatorCall.arguments[0])
        val conversionType = data.embedType(typeOperatorCall.conversionTypeRef.coneType)
        return when (typeOperatorCall.operation) {
            FirOperation.IS -> Is(argument, conversionType)
            FirOperation.NOT_IS -> Not(Is(argument, conversionType))
            FirOperation.AS -> Cast(argument, conversionType).withAccessInvariants()
            FirOperation.SAFE_AS -> SafeCast(argument, conversionType).withAccessInvariants()
            else -> handleUnimplementedElement("Can't embed type operator ${typeOperatorCall.operation}.", data)
        }
    }

    override fun visitLambdaArgumentExpression(
        lambdaArgumentExpression: FirLambdaArgumentExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        // TODO: check whether there are other cases.
        val function = (lambdaArgumentExpression.expression as FirAnonymousFunctionExpression).anonymousFunction
        return LambdaExp(data.embedFunctionSignature(function.symbol), function, data)
    }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: StmtConversionContext): ExpEmbedding {
        val (catchData, tryBody) = data.withCatches(tryExpression.catches) { catchData ->
            withNewScope {
                val jumps = catchData.blocks.map { catchBlock -> NonDeterministically(Goto(catchBlock.entryLabel)) }
                val body = convert(tryExpression.tryBlock)
                GotoChainNode(null, Block(jumps + listOf(body) + jumps), catchData.exitLabel)
            }
        }
        val catches = catchData.blocks.map { catchBlock ->
            data.withNewScope {
                val parameter = catchBlock.firCatch.parameter
                // The value is the thrown exception, which we do not know, hence we do not initialise the exception variable.
                val paramDecl = declareLocalProperty(parameter.symbol, null)
                GotoChainNode(
                    catchBlock.entryLabel,
                    Block(
                        paramDecl,
                        convert(catchBlock.firCatch.block)
                    ),
                    catchData.exitLabel
                )
            }
        }
        return Block(listOf(tryBody) + catches + LabelExp(catchData.exitLabel))
    }

    override fun visitElvisExpression(
        elvisExpression: FirElvisExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val lhs = data.convert(elvisExpression.lhs)
        val rhs = data.convert(elvisExpression.rhs)
        val expType = data.embedType(elvisExpression.resolvedType)
        return Elvis(lhs, rhs, expType)
    }

    override fun visitSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: StmtConversionContext,
    ): ExpEmbedding {
        val selector = safeCallExpression.selector
        val receiver = data.convert(safeCallExpression.receiver)
        val expType = data.embedType(safeCallExpression.resolvedType)
        val checkedSafeCallSubjectType = data.embedType(safeCallExpression.checkedSubjectRef.value.resolvedType)

        val storedReceiverDecl = data.declareAnonVar(receiver.type, receiver)
        return Block(
            storedReceiverDecl,
            If(
                storedReceiverDecl.variable.notNullCmp(),
                data.withCheckedSafeCallSubject(storedReceiverDecl.variable.withType(checkedSafeCallSubjectType)) { convert(selector) },
                expType.getNullable().nullVal,
                expType
            ),
        )
    }

    override fun visitCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: StmtConversionContext,
    ): ExpEmbedding = data.checkedSafeCallSubject
        ?: throw IllegalArgumentException("Trying to resolve checked subject $checkedSafeCallSubject which was not captured in StmtConversionContext")

    private fun handleUnimplementedElement(msg: String, data: StmtConversionContext): ExpEmbedding =
        when (data.config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                TODO(msg)
            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                data.errorCollector.addMinorError(msg)
                ErrorExp
            }
        }
}

object StmtConversionVisitorExceptionWrapper : FirVisitor<ExpEmbedding, StmtConversionContext>() {
    override fun visitElement(element: FirElement, data: StmtConversionContext): ExpEmbedding {
        try {
            return element.accept(StmtConversionVisitor, data)
        } catch (e: Exception) {
            data.errorCollector.addErrorInfo("... while converting ${element.source.text}")
            throw e
        }
    }
}
