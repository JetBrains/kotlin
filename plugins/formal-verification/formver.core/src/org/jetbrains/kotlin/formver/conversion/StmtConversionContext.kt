/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.calleeSymbol
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.viper.MangledName
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

    fun withInlineContext(
        inlineSignature: FullNamedFunctionSignature,
        returnVarName: MangledName,
        substitutionParams: Map<Name, SubstitutionItem>,
    ): StmtConversionContext<RTC>

    fun withLambdaContext(
        inlineSignature: FullNamedFunctionSignature,
        returnVarName: MangledName,
        substitutionParams: Map<Name, SubstitutionItem>,
        scopedNames: Map<Name, Int>,
    ): StmtConversionContext<RTC>

    fun withResult(type: TypeEmbedding, action: StmtConversionContext<VarResultTrackingContext>.() -> Unit): VariableEmbedding {
        val ctx = withResult(type)
        ctx.action()
        return ctx.resultCtx.resultVar
    }

    fun withWhenSubject(subject: VariableEmbedding?, action: (StmtConversionContext<RTC>) -> Unit)
    fun inNewScope(action: (StmtConversionContext<RTC>) -> ExpEmbedding): ExpEmbedding
    fun addScopedName(name: Name)
    fun getScopeDepth(name: Name): Int
    fun getScopedNames(): Map<Name, Int>
}

fun <RTC : ResultTrackingContext> StmtConversionContext<RTC>.embedPropertyAccess(symbol: FirPropertyAccessExpression): PropertyAccessEmbedding =
    when (val calleeSymbol = symbol.calleeSymbol) {
        is FirValueParameterSymbol -> embedValueParameter(calleeSymbol)
        is FirPropertySymbol ->
            when (val receiverFir = symbol.dispatchReceiver) {
                null -> embedLocalProperty(calleeSymbol, getScopeDepth(calleeSymbol.name))
                else -> ClassPropertyAccess(convert(receiverFir), embedGetter(calleeSymbol), embedSetter(calleeSymbol))
            }
        else -> throw Exception("Property access symbol $calleeSymbol has unsupported type.")
    }

fun <RTC : ResultTrackingContext> StmtConversionContext<RTC>.embedProperty(symbol: FirPropertySymbol): VariableEmbedding =
    embedLocalProperty(symbol, getScopeDepth(symbol.name))

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

fun StmtConversionContext<ResultTrackingContext>.getFunctionCallSubstitutionItems(
    args: List<ExpEmbedding>,
): List<SubstitutionItem> = args.map { exp ->
    when (exp) {
        is VariableEmbedding -> SubstitutionName(exp.name)
        is LambdaExp -> SubstitutionLambda(exp)
        else -> {
            val result = withResult(exp.type) {
                resultCtx.resultVar.setValue(exp, this)
            }
            SubstitutionName(result.name)
        }
    }
}
