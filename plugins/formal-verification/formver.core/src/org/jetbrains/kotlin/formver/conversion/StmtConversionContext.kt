/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.calleeSymbol
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Label
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.name.Name

/**
 * Interface for statement conversion.
 *
 * Naming convention:
 * - Functions that return a new `StmtConversionContext` should describe what change they make (`addResult`, `removeResult`...)
 * - Functions that take a lambda to execute should describe what extra state the lambda will have (`withResult`...)
 */
interface StmtConversionContext<out RTC : ResultTrackingContext> : MethodConversionContext, SeqnBuildContext, ResultTrackingContext {
    val resultCtx: RTC
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
    fun store(exp: ExpEmbedding): VariableEmbedding

    fun removeResult(): StmtConversionContext<NoopResultTracker>
    fun addResult(type: TypeEmbedding): StmtConversionContext<VarResultTrackingContext>

    fun withNewScopeToBlock(action: StmtConversionContext<RTC>.() -> Unit): Stmt.Seqn
    fun <R> withMethodCtx(factory: MethodContextFactory, action: StmtConversionContext<RTC>.() -> R): R

    fun <R> withFreshWhile(action: StmtConversionContext<RTC>.() -> R): R
    fun <R> withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext<RTC>.() -> R): R
    fun <R> withCheckedSafeCallSubject(subject: ExpEmbedding?, action: StmtConversionContext<RTC>.() -> R): R
    fun withCatches(
        catches: List<FirCatch>,
        action: StmtConversionContext<RTC>.(catchBlockListData: CatchBlockListData) -> Unit,
    ): CatchBlockListData
}

fun <RTC : ResultTrackingContext> StmtConversionContext<RTC>.nonDeterministically(action: StmtConversionContext<RTC>.() -> Unit) {
    val branchVar = freshAnonVar(BooleanTypeEmbedding)
    addDeclaration(branchVar.toLocalVarDecl())
    addStatement(Stmt.If(branchVar.toViper(), withNewScopeToBlock(action), Stmt.Seqn()))
}

fun StmtConversionContext<ResultTrackingContext>.convertAndStore(exp: FirExpression): VariableEmbedding = store(convert(exp))

fun StmtConversionContext<ResultTrackingContext>.convertAndCapture(exp: FirExpression) {
    resultCtx.capture(convert(exp))
}

fun StmtConversionContext<ResultTrackingContext>.declareLocal(
    name: Name,
    type: TypeEmbedding,
    initializer: ExpEmbedding?,
): VariableEmbedding {
    registerLocalPropertyName(name)
    val varEmb = VariableEmbedding(resolveLocalPropertyName(name), type)
    addDeclaration(varEmb.toLocalVarDecl())
    initializer?.let { varEmb.setValue(it, this) }
    return varEmb
}

fun StmtConversionContext<ResultTrackingContext>.embedPropertyAccess(symbol: FirPropertyAccessExpression): PropertyAccessEmbedding =
    when (val calleeSymbol = symbol.calleeSymbol) {
        is FirValueParameterSymbol -> embedParameter(calleeSymbol).asPropertyAccess()
        is FirPropertySymbol ->
            when {
                symbol.dispatchReceiver != null -> {
                    ClassPropertyAccess(convert(symbol.dispatchReceiver!!), embedGetter(calleeSymbol), embedSetter(calleeSymbol))
                }
                symbol.extensionReceiver != null -> {
                    ClassPropertyAccess(convert(symbol.extensionReceiver!!), embedGetter(calleeSymbol), embedSetter(calleeSymbol))
                }
                else -> embedLocalProperty(calleeSymbol)
            }
        else -> throw IllegalStateException("Property access symbol $calleeSymbol has unsupported type.")
    }

@OptIn(SymbolInternals::class)
fun <RTC : ResultTrackingContext> StmtConversionContext<RTC>.embedGetter(symbol: FirPropertySymbol): GetterEmbedding? =
    when (val getter = symbol.fir.getter) {
        null, is FirDefaultPropertyGetter -> getField(symbol)?.let { BackingFieldGetter(it) }
        else -> CustomGetter(embedFunction(getter.symbol))
    }

@OptIn(SymbolInternals::class)
fun <RTC : ResultTrackingContext> StmtConversionContext<RTC>.embedSetter(symbol: FirPropertySymbol): SetterEmbedding? =
    when (val setter = symbol.fir.setter) {
        null, is FirDefaultPropertySetter -> getField(symbol)?.let { BackingFieldSetter(it) }
        else -> CustomSetter(embedFunction(setter.symbol))
    }

fun StmtConversionContext<ResultTrackingContext>.withResult(
    type: TypeEmbedding,
    action: StmtConversionContext<VarResultTrackingContext>.() -> Unit,
): VariableEmbedding {
    val ctx = addResult(type)
    ctx.action()
    return ctx.resultCtx.resultVar
}

fun <RTC : ResultTrackingContext, R> StmtConversionContext<RTC>.withNewScope(action: StmtConversionContext<RTC>.() -> R): R {
    // Funny, if we could put a contract on `withNewScopeToBlock` we could do this with `val`, but we can't because of inheritance.
    var result: R? = null
    val block = withNewScopeToBlock {
        result = action()
    }
    // NOTE: Putting the block inside the then branch of an if-true statement is a little hack to make Viper respect the scoping
    addStatement(Stmt.If(Exp.BoolLit(true), block, Stmt.Seqn()))
    return result!!
}

fun StmtConversionContext<ResultTrackingContext>.getInlineFunctionCallArgs(
    args: List<ExpEmbedding>,
): List<ExpEmbedding> = args.map { exp ->
    when (exp) {
        is VariableEmbedding -> exp
        is LambdaExp -> exp
        else -> withResult(exp.type) {
            resultCtx.resultVar.setValue(exp, this)
        }
    }
}

fun StmtConversionContext<ResultTrackingContext>.insertInlineFunctionCall(
    calleeSignature: FunctionSignature,
    paramNames: List<Name>,
    args: List<ExpEmbedding>,
    body: FirBlock,
    parentCtx: MethodConversionContext? = null,
    returnPointName: String? = null,
): ExpEmbedding = withResult(calleeSignature.returnType) {
    val callArgs = getInlineFunctionCallArgs(args)
    val subs = paramNames.zip(callArgs).toMap()
    val returnLabelName = returnLabelNameProducer.getFresh()
    val methodCtxFactory = MethodContextFactory(
        calleeSignature,
        InlineParameterResolver(this.resultCtx.resultVar.name, returnLabelName, subs),
        parentCtx,
        returnPointName
    )
    withMethodCtx(methodCtxFactory) {
        convert(body)
        addDeclaration(returnLabel.toDecl())
        addStatement(returnLabel.toStmt())
    }
}
