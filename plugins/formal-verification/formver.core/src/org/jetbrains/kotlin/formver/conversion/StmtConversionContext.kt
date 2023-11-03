/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.calleeSymbol
import org.jetbrains.kotlin.formver.embeddings.ClassPropertyAccess
import org.jetbrains.kotlin.formver.embeddings.PropertyAccessEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.asPropertyAccess
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.*
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.name.Name

/**
 * Interface for statement conversion.
 *
 * Naming convention:
 * - Functions that return a new `StmtConversionContext` should describe what change they make (`addResult`, `removeResult`...)
 * - Functions that take a lambda to execute should describe what extra state the lambda will have (`withResult`...)
 */
interface StmtConversionContext : MethodConversionContext {
    val whenSubject: VariableEmbedding?

    /**
     * In a safe call `callSubject?.foo()` we evaluate the call subject first to check for nullness.
     * In case it is not null, we evaluate the call to `callSubject.foo()`. Here we don't want to evaluate
     * the `callSubject` again to we store it in the `StmtConversionContext`.
     */
    val checkedSafeCallSubject: ExpEmbedding?
    val activeCatchLabels: List<Label>

    fun continueLabel(targetName: String? = null): Label
    fun breakLabel(targetName: String? = null): Label
    fun addLoopName(targetName: String)
    fun convert(stmt: FirStatement): ExpEmbedding

    fun <R> withNewScope(action: StmtConversionContext.() -> R): R
    fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext.() -> R): R

    fun <R> withFreshWhile(label: FirLabel?, action: StmtConversionContext.() -> R): R
    fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext.() -> R): R
    fun <R> withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext.(catchBlockListData: CatchBlockListData) -> R,
    ): Pair<CatchBlockListData, R>
}

fun StmtConversionContext.declareLocal(
    name: Name,
    type: TypeEmbedding,
    initializer: ExpEmbedding?,
): Declare {
    registerLocalPropertyName(name)
    val varEmb = VariableEmbedding(resolveLocalPropertyName(name), type)
    return Declare(varEmb, initializer)
}

fun StmtConversionContext.declareAnonLocal(
    type: TypeEmbedding,
    initializer: ExpEmbedding?,
): Declare = Declare(freshAnonVar(type), initializer)

fun StmtConversionContext.embedPropertyAccess(accessExpression: FirPropertyAccessExpression): PropertyAccessEmbedding =
    when (val calleeSymbol = accessExpression.calleeSymbol) {
        is FirValueParameterSymbol -> embedParameter(calleeSymbol).asPropertyAccess()
        is FirPropertySymbol -> when {
            accessExpression.dispatchReceiver != null -> {
                val property = embedProperty(calleeSymbol)
                ClassPropertyAccess(convert(accessExpression.dispatchReceiver!!), property)
            }
            accessExpression.extensionReceiver != null -> {
                val property = embedProperty(calleeSymbol)
                ClassPropertyAccess(convert(accessExpression.extensionReceiver!!), property)
            }
            else -> embedLocalProperty(calleeSymbol)
        }
        else -> throw IllegalStateException("Property access symbol $calleeSymbol has unsupported type.")
    }

fun StmtConversionContext.getInlineFunctionCallArgs(
    args: List<ExpEmbedding>,
): Pair<List<Declare>, List<ExpEmbedding>> {
    val declarations = mutableListOf<Declare>()
    val storedArgs = args.map { arg ->
        when (arg.ignoringMetaNodes()) {
            is VariableEmbedding, is LambdaExp -> arg
            else -> {
                val paramVarDecl = declareAnonLocal(arg.type, arg)
                declarations.add(paramVarDecl)
                paramVarDecl.variable
            }
        }
    }
    return Pair(declarations, storedArgs)
}

fun StmtConversionContext.insertInlineFunctionCall(
    calleeSignature: FunctionSignature,
    paramNames: List<Name>,
    args: List<ExpEmbedding>,
    body: FirBlock,
    returnTargetName: String?,
    parentCtx: MethodConversionContext? = null,
): ExpEmbedding {
    // TODO: It seems like it may be possible to avoid creating a local here, but it is not clear how.
    val returnTarget = returnTargetProducer.getFresh(calleeSignature.returnType)
    val (declarations, callArgs) = getInlineFunctionCallArgs(args)
    val subs = paramNames.zip(callArgs).toMap()
    val methodCtxFactory = MethodContextFactory(
        calleeSignature,
        InlineParameterResolver(subs, returnTargetName, returnTarget),
        parentCtx,
    )
    return withMethodCtx(methodCtxFactory) {
        Block(
            buildList {
                add(Declare(returnTarget.variable, null))
                addAll(declarations)
                add(FunctionExp(null, convert(body), returnTarget.label))
                add(returnTarget.variable)
            }
        )
    }
}

