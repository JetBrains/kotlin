/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.calleeSymbol
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.name.Name

interface StmtConversionContext<out RTC : ResultTrackingContext> : MethodConversionContext, SeqnBuildContext, ResultTrackingContext,
    WhileStackContext<RTC> {
    val resultCtx: RTC
    val whenSubject: VariableEmbedding?

    fun convert(stmt: FirStatement): ExpEmbedding
    fun convertAndStore(exp: FirExpression): VariableEmbedding

    fun convertAndCapture(exp: FirExpression) {
        resultCtx.capture(convert(exp))
    }

    fun newBlock(): StmtConversionContext<RTC>
    fun withoutResult(): StmtConversionContext<NoopResultTracker>
    fun withResult(type: TypeEmbedding): StmtConversionContext<VarResultTrackingContext>

    fun withMethodContext(newCtx: MethodConversionContext): StmtConversionContext<RTC>

    fun withResult(type: TypeEmbedding, action: StmtConversionContext<VarResultTrackingContext>.() -> Unit): VariableEmbedding {
        val ctx = withResult(type)
        ctx.action()
        return ctx.resultCtx.resultVar
    }

    fun withWhenSubject(subject: VariableEmbedding?, action: StmtConversionContext<RTC>.() -> Unit)
    fun inNewScope(action: StmtConversionContext<RTC>.() -> ExpEmbedding): ExpEmbedding
}

fun <RTC : ResultTrackingContext> StmtConversionContext<RTC>.embedPropertyAccess(symbol: FirPropertyAccessExpression): PropertyAccessEmbedding =
    when (val calleeSymbol = symbol.calleeSymbol) {
        is FirValueParameterSymbol -> embedParameter(calleeSymbol) as VariableEmbedding
        is FirPropertySymbol ->
            when (val receiverFir = symbol.dispatchReceiver) {
                null -> embedLocalProperty(calleeSymbol)
                else -> ClassPropertyAccess(convert(receiverFir), embedGetter(calleeSymbol), embedSetter(calleeSymbol))
            }
        else -> throw Exception("Property access symbol $calleeSymbol has unsupported type.")
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
): ExpEmbedding = withResult(calleeSignature.returnType) {
    val callArgs = getInlineFunctionCallArgs(args)
    val subs = paramNames.zip(callArgs).toMap()
    val returnLabelName = ReturnLabelName(newWhileIndex())
    val newMethodCtx = MethodConverter(
        this, calleeSignature,
        InlineParameterResolver(this.resultCtx.resultVar.name, returnLabelName, subs),
        parentCtx
    )
    val inlineCtx = this.newBlock().withMethodContext(newMethodCtx)
    inlineCtx.convert(body)
    inlineCtx.addDeclaration(inlineCtx.returnLabel.toDecl())
    inlineCtx.addStatement(inlineCtx.returnLabel.toStmt())
    // NOTE: Putting the block inside the then branch of an if-true statement is a little hack to make Viper respect the scoping
    addStatement(Stmt.If(Exp.BoolLit(true), inlineCtx.block, Stmt.Seqn(listOf(), listOf())))
}
