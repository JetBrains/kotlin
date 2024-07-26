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
        inlineMethod: MethodEmbedding,
        returnVarName: MangledName,
        substitutionParams: Map<Name, SubstitutionItem>,
    ): StmtConversionContext<RTC>

    fun withResult(type: TypeEmbedding, action: StmtConversionContext<VarResultTrackingContext>.() -> Unit): VariableEmbedding {
        val ctx = withResult(type)
        ctx.action()
        return ctx.resultCtx.resultVar
    }

    fun withWhenSubject(subject: VariableEmbedding?, action: (StmtConversionContext<RTC>) -> Unit)
}

fun <RTC : ResultTrackingContext> StmtConversionContext<RTC>.embedPropertyAccess(symbol: FirPropertyAccessExpression): PropertyAccessEmbedding =
    when (val calleeSymbol = symbol.calleeSymbol) {
        is FirValueParameterSymbol -> LocalPropertyAccess(embedValueParameter(calleeSymbol))
        is FirPropertySymbol ->
            when (val receiverFir = symbol.dispatchReceiver) {
                null -> LocalPropertyAccess(embedLocalProperty(calleeSymbol))
                else -> ClassPropertyAccess(convert(receiverFir), embedGetter(calleeSymbol), embedSetter(calleeSymbol))
            }
        else -> throw Exception("Property access symbol $calleeSymbol has unsupported type.")
    }

@OptIn(SymbolInternals::class)
fun <RTC : ResultTrackingContext> StmtConversionContext<RTC>.embedGetter(symbol: FirPropertySymbol): GetterEmbedding? =
    when (val getter = symbol.fir.getter) {
        null -> null
        is FirDefaultPropertyGetter -> BackingFieldGetter(
            VariableEmbedding(
                symbol.callableId.embedName(),
                embedType(symbol.resolvedReturnType)
            )
        )
        else -> CustomGetter(embedFunction(getter.symbol))
    }

@OptIn(SymbolInternals::class)
fun <RTC : ResultTrackingContext> StmtConversionContext<RTC>.embedSetter(symbol: FirPropertySymbol): SetterEmbedding? =
    when (val setter = symbol.fir.setter) {
        null -> null
        is FirDefaultPropertySetter -> BackingFieldSetter(
            VariableEmbedding(
                symbol.callableId.embedName(),
                embedType(symbol.resolvedReturnType)
            )
        )
        else -> CustomSetter(embedFunction(setter.symbol))
    }
