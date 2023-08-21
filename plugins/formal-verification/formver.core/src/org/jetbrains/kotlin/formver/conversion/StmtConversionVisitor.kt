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
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.formver.UnsupportedFeatureBehaviour
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.viper.ast.AccessPredicate
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.domains.NullableDomain
import org.jetbrains.kotlin.formver.viper.domains.UnitDomain
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
        handleUnimplementedElement("Not yet implemented for $element (${element.source.text})", data)

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: StmtConversionContext): Exp {
        val expr = returnExpression.result.accept(this, data)
        // TODO: respect return-based control flow
        val returnVar = data.signature.returnVar
        data.addStatement(Stmt.LocalVarAssign(returnVar.toLocalVar(), expr.withType(returnVar.viperType)))
        return UnitDomain.element
    }

    override fun visitBlock(block: FirBlock, data: StmtConversionContext): Exp =
        // We ignore the accumulator: we just want to get the result of the last expression.
        block.statements.fold<FirStatement, Exp>(UnitDomain.element) { _, it -> it.accept(this, data) }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: StmtConversionContext): Exp =
        when (constExpression.kind) {
            ConstantValueKind.Int -> Exp.IntLit((constExpression.value as Long).toInt())
            ConstantValueKind.Boolean -> Exp.BoolLit(constExpression.value as Boolean)
            ConstantValueKind.Null -> NullableDomain.nullVal((data.embedType(constExpression) as NullableTypeEmbedding).elementType.type)
            else -> handleUnimplementedElement("Constant Expression of type ${constExpression.kind} is not yet implemented.", data)
        }

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: StmtConversionContext): Exp =
        // TODO: find a way to not evaluate subject multiple times if it is a function call
        whenSubjectExpression.whenRef.value.subject!!.accept(this, data)

    private fun convertWhenBranches(whenBranches: Iterator<FirWhenBranch>, data: StmtConversionContext, cvar: VariableEmbedding?) {
        // NOTE: I think that this will also work with "in" or "is" conditions when implemented, but I'm not 100% sure
        if (!whenBranches.hasNext()) return

        val branch = whenBranches.next()

        // Note that only the last condition can be a FirElseIfTrue
        if (branch.condition is FirElseIfTrueCondition) {
            val result = branch.result.accept(this, data)
            cvar?.let { data.addStatement(Stmt.LocalVarAssign(cvar.toLocalVar(), result.withType(cvar.viperType))) }
        } else {
            val cond = branch.condition.accept(this, data)
            val thenCtx = StmtConverter(data)
            val thenResult = branch.result.accept(this, thenCtx)
            cvar?.let { thenCtx.addStatement(Stmt.LocalVarAssign(cvar.toLocalVar(), thenResult.withType(cvar.viperType))) }
            val elseCtx = StmtConverter(data)
            convertWhenBranches(whenBranches, elseCtx, cvar)
            data.addStatement(Stmt.If(cond, thenCtx.block, elseCtx.block))
        }
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: StmtConversionContext): Exp {
        val cvar = if (whenExpression.usedAsExpression) {
            data.newAnonVar(data.embedType(whenExpression))
        } else {
            null
        }
        cvar?.let { data.addDeclaration(cvar.toLocalVarDecl()) }
        convertWhenBranches(whenExpression.branches.iterator(), data, cvar)
        return cvar?.toLocalVar() ?: UnitDomain.element
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: StmtConversionContext,
    ): Exp {
        val symbol = propertyAccessExpression.calleeReference.toResolvedBaseSymbol()!!
        val type = data.embedType(propertyAccessExpression)
        return when (symbol) {
            is FirValueParameterSymbol -> VariableEmbedding(symbol.callableId.embedName(), type).toLocalVar()
            is FirPropertySymbol -> {
                val varEmbedding = VariableEmbedding(symbol.callableId.embedName(), type)
                if (symbol.isLocal) {
                    return varEmbedding.toLocalVar()
                } else {
                    val receiver = propertyAccessExpression.dispatchReceiver.accept(this, data)
                    val fieldAccess = Exp.FieldAccess(receiver, varEmbedding.toField())
                    val accPred = AccessPredicate.FieldAccessPredicate(fieldAccess, PermExp.FullPerm())
                    val anon = data.newAnonVar(varEmbedding.type)

                    data.addDeclaration(anon.toLocalVarDecl())
                    // Inhale permission for the field before reading it.
                    data.addStatement(Stmt.Inhale(accPred))
                    // Access the field and assign the value to a newly created anonymous variable.
                    data.addStatement(Stmt.assign(anon.toLocalVar(), fieldAccess))
                    // Exhale permission for the field and return the anonymous variable value
                    data.addStatement(Stmt.Exhale(accPred))
                    return anon.toLocalVar()
                }
            }
            else -> handleUnimplementedElement("Property access ${propertyAccessExpression.source} not implemented", data)
        }
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: StmtConversionContext): Exp {
        if (equalityOperatorCall.arguments.size != 2) {
            throw IllegalArgumentException("Invalid equality comparison $equalityOperatorCall, can only compare 2 elements.")
        }
        val left = equalityOperatorCall.arguments[0].accept(this, data)
        val right = equalityOperatorCall.arguments[1].accept(this, data)

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
                Exp.NeCmp(left, NullableDomain.nullVal(leftType.elementType.type)),
                // TODO: Replace the Eq comparison with a member call function to `left.equals`
                Exp.EqCmp(left.withType(leftType.elementType.type), right.withType(leftType.elementType.type))
            )
        } else if (leftType is NullableTypeEmbedding && rightType is NullableTypeEmbedding) {
            Exp.Or(
                Exp.And(
                    Exp.EqCmp(left, NullableDomain.nullVal(leftType.elementType.type)),
                    Exp.EqCmp(right, NullableDomain.nullVal(rightType.elementType.type)),
                ),
                // TODO: Replace the Eq comparison with a member call function to `left.equals`
                Exp.EqCmp(left.withType(leftType.elementType.type), right.withType(leftType.elementType.type))
            )
        } else {
            // TODO: Replace the Eq comparison with a member call function to `left.equals`
            Exp.EqCmp(left, right.withType(left.type))
        }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StmtConversionContext): Exp {
        val id = functionCall.calleeReference.toResolvedCallableSymbol()!!.callableId
        val specialFunc = SpecialFunctions.byCallableId[id]
        val getArgs = { getFunctionCallArguments(functionCall).map { it.accept(this, data) } }
        if (specialFunc != null) {
            if (specialFunc !is SpecialFunctionImplementation) return UnitDomain.element
            return specialFunc.convertCall(getArgs(), data)
        }

        val symbol = functionCall.calleeReference.resolved!!.resolvedSymbol
        val calleeSig = when (symbol) {
            is FirNamedFunctionSymbol -> data.add(symbol)
            is FirConstructorSymbol -> data.add(symbol)
            else -> TODO("Are there any other possible cases?")
        }

        val returnVar = data.newAnonVar(calleeSig.returnType)
        val returnExp = returnVar.toLocalVar()
        val args = getArgs().zip(calleeSig.formalArgs).map { (arg, formalArg) -> arg.withType(formalArg.viperType) }
        data.addDeclaration(returnVar.toLocalVarDecl())
        data.addStatement(Stmt.MethodCall(calleeSig.name.mangled, args, listOf(returnExp)))

        return returnExp
    }

    override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: StmtConversionContext): Exp {
        val args = getFunctionCallArguments(implicitInvokeCall).map { it.accept(this, data) }
        val retType = implicitInvokeCall.calleeReference.toResolvedCallableSymbol()!!.resolvedReturnType
        val returnVar = data.newAnonVar(data.embedType(retType))
        val returnExp = returnVar.toLocalVar()
        data.addDeclaration(returnVar.toLocalVarDecl())
        // NOTE: Since it is only relevant to update the number of times that a function object is called,
        // the function call invocation is intentionally not assigned to the return variable
        data.addStatement(Stmt.MethodCall(InvokeFunctionObjectName.mangled, args.take(1), listOf()))
        return returnExp
    }

    private fun getFunctionCallArguments(functionCall: FirFunctionCall): List<FirExpression> {
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
            handleUnimplementedElement("Non-local property ${property.source} is not yet implemented.", data)
        }
        val cvar = VariableEmbedding(symbol.callableId.embedName(), data.embedType(type))
        val propInitializer = property.initializer
        val initializer = propInitializer?.accept(this, data)
        data.addDeclaration(cvar.toLocalVarDecl())
        initializer?.let { data.addStatement(Stmt.LocalVarAssign(cvar.toLocalVar(), it.withType(cvar.viperType))) }
        return UnitDomain.element
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: StmtConversionContext): Exp {
        val condVar = data.newAnonVar(BooleanTypeEmbedding)
        data.addDeclaration(condVar.toLocalVarDecl())
        val cond = whileLoop.condition.accept(this, data)
        data.addStatement(Stmt.LocalVarAssign(condVar.toLocalVar(), cond))

        val bodyStmtConversionContext = StmtConverter(data)
        bodyStmtConversionContext.convertAndAppend(whileLoop.block)
        val updatedCond = whileLoop.condition.accept(this, bodyStmtConversionContext)
        bodyStmtConversionContext.addStatement(Stmt.LocalVarAssign(condVar.toLocalVar(), updatedCond))

        data.addStatement(Stmt.While(condVar.toLocalVar(), invariants = emptyList(), bodyStmtConversionContext.block))
        return UnitDomain.element
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: StmtConversionContext): Exp {
        // It is not entirely clear whether we can get away with ignoring the distinction between
        // lvalues and rvalues, but let's try to at first, and we'll fix it later if it turns out
        // not to work.
        val convertedLValue = variableAssignment.lValue.accept(this, data)
        val convertedRValue = variableAssignment.rValue.accept(this, data)
        data.addStatement(Stmt.assign(convertedLValue, convertedRValue.withType(convertedLValue.type)))
        return UnitDomain.element
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: StmtConversionContext): Exp {
        val exp = smartCastExpression.originalExpression.accept(this, data)
        val newType = smartCastExpression.smartcastType.coneType
        return exp.withType(data.embedType(newType).type)
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

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: StmtConversionContext): Exp =
        data.signature.receiver?.toLocalVar()
            ?: throw IllegalArgumentException("Can't resolve the 'this' receiver since the function does not have one.")

    private fun handleUnimplementedElement(msg: String, data: StmtConversionContext): Exp =
        when (data.config.behaviour) {
            UnsupportedFeatureBehaviour.THROW_EXCEPTION ->
                TODO(msg)
            UnsupportedFeatureBehaviour.ASSUME_UNREACHABLE -> {
                // TODO: This is not perfect, sa the resulting Viper may not typecheck.
                System.err.println(msg) // hack for while we're actively developing this to see what we're missing
                data.addStatement(Stmt.Assume(Exp.BoolLit(false)))
                UnitDomain.element
            }
        }
}