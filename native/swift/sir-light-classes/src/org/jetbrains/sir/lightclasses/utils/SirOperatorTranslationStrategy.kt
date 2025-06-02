/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTranslationResult
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.sir.lightclasses.nodes.SirBinaryMathOperatorFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirUnaryMathOperatorFromKtSymbol

public sealed class SirOperatorTranslationStrategy(public val kaSymbol: KaSymbol) {
    public companion object {
        public operator fun invoke(kaSymbol: KaSymbol): SirOperatorTranslationStrategy? =
            when (val name = (kaSymbol as? KaNamedFunctionSymbol)?.takeIf { it.isOperator }?.name?.identifier) {
                null -> null
                "not", "unaryPlus", "unaryMinus", "inc", "dec" -> UnaryMathOperator(kaSymbol)
                "plus", "minus", "times", "div", "rem" -> BinaryMathOperator(kaSymbol)
                "plusAssign", "minusAssign", "timesAssign", "divAssign", "remAssign" -> CompoundAssignmentBinaryMathOperator(kaSymbol)
                "getValue", "setValue", "provideDelegate" -> null // property wrapper?
                "rangeTo", "rangeUntil" -> null // range operator
                "get", "set" -> SubscriptAccessor(kaSymbol)
                "iterator" -> null
                "contains" -> null
                "equals" -> null
                "compareTo" -> Comparison(kaSymbol)
                "invoke" -> Invoke(kaSymbol)
                else if name.startsWith("component") -> null // componentN – ignored
                else -> error("Unknown operator name: $name in SirOperatorTranslationStrategy.")
            }
    }

    public open fun translate(sirSession: SirSession): SirTranslationResult = SirTranslationResult.Untranslatable(KotlinSource(kaSymbol))

    public class UnaryMathOperator(symbol: KaSymbol) : SirOperatorTranslationStrategy(symbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            return SirUnaryMathOperatorFromKtSymbol(kaSymbol as KaNamedFunctionSymbol, sirSession = sirSession)
                .let(SirTranslationResult::RegularFunction)
        }
    }

    public class BinaryMathOperator(symbol: KaSymbol): SirOperatorTranslationStrategy(symbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            return SirBinaryMathOperatorFromKtSymbol(kaSymbol as KaNamedFunctionSymbol, sirSession = sirSession)
                .let(SirTranslationResult::RegularFunction)
        }
    }

    public class CompoundAssignmentBinaryMathOperator(symbol: KaSymbol): SirOperatorTranslationStrategy(symbol)

    public class SubscriptAccessor(symbol: KaSymbol): SirOperatorTranslationStrategy(symbol)
    public class Invoke(symbol: KaSymbol): SirOperatorTranslationStrategy(symbol)
    public class Comparison(symbol: KaSymbol): SirOperatorTranslationStrategy(symbol)
}