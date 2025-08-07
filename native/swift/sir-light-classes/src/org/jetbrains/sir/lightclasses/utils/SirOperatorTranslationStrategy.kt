/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.components.containingSymbol
import org.jetbrains.kotlin.analysis.api.components.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTranslationResult
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.kotlin.sir.util.allParameters
import org.jetbrains.sir.lightclasses.nodes.SirBinaryMathOperatorTrampolineFunction
import org.jetbrains.sir.lightclasses.nodes.SirComparisonOperatorTrampolineFunction
import org.jetbrains.sir.lightclasses.nodes.SirFunctionFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirRenamedFunction
import org.jetbrains.sir.lightclasses.nodes.SirSubscriptTrampoline
import org.jetbrains.sir.lightclasses.nodes.SirUnaryMathOperatorTrampolineFunction

public sealed class SirOperatorTranslationStrategy(public val kaSymbol: KaNamedFunctionSymbol) {
    public companion object {
        public operator fun invoke(kaSymbol: KaSymbol): SirOperatorTranslationStrategy? = (kaSymbol as? KaNamedFunctionSymbol)
            ?.takeIf { it.isOperator }
            ?.takeIf { it.receiverParameter == null } // extension operators are not supported yet
            ?.let { symbol ->
                when (val name = symbol.name.identifier) {
                    // Unary Math
                    "not" -> AsUnaryMathOperator(kaSymbol, "!")
                    "unaryPlus" -> AsUnaryMathOperator(kaSymbol, "+")
                    "unaryMinus" -> AsUnaryMathOperator(kaSymbol, "-")
                    "inc", "dec" -> AsIsWithAdditions(kaSymbol) // swift deprecate ++ and -- operators in 2.2 and removed them in 3.0.

                    // Binary Math
                    "plus" -> AsBinaryMathOperator(kaSymbol, "+")
                    "minus" -> AsBinaryMathOperator(kaSymbol, "-")
                    "times" -> AsBinaryMathOperator(kaSymbol, "*")
                    "div" -> AsBinaryMathOperator(kaSymbol, "/")
                    "rem" -> AsBinaryMathOperator(kaSymbol, "%")

                    // Compound Assignment Binary Math
                    "plusAssign" -> AsCompoundAssignmentBinaryMathOperator(kaSymbol, "+=")
                    "minusAssign" -> AsCompoundAssignmentBinaryMathOperator(kaSymbol, "-=")
                    "timesAssign" -> AsCompoundAssignmentBinaryMathOperator(kaSymbol, "*=")
                    "divAssign" -> AsCompoundAssignmentBinaryMathOperator(kaSymbol, "/=")
                    "remAssign" -> AsCompoundAssignmentBinaryMathOperator(kaSymbol, "%=")

                    // Unsupported; possibly, property wrappers?
                    "getValue", "setValue", "provideDelegate" -> Skip(kaSymbol)

                    // Unsupported; ranges
                    "rangeTo", "rangeUntil" -> AsIsWithAdditions(kaSymbol)

                    // Unsupported; subscripts
                    "get", "set" -> AsSubscriptAccessor(kaSymbol)

                    // Unsupported; iterators
                    "iterator", "next", "hasNext" -> AsIsWithAdditions(kaSymbol)

                    // Misc
                    "contains" -> AsIsWithAdditions(kaSymbol) { listOf(SirBinaryMathOperatorTrampolineFunction(it, "~=")) }
                    "equals" -> AsIsWithAdditions(kaSymbol) { listOf(SirBinaryMathOperatorTrampolineFunction(it, "==")) }
                    "compareTo" -> AsComparisonOperator(kaSymbol)
                    "invoke" -> AsInvokeOperator(kaSymbol)

                    else if name.startsWith("component") -> Skip(kaSymbol) // componentN – ignored
                    else -> error("Unknown operator name: $name in SirOperatorTranslationStrategy.")
                }
            }
    }

    public open fun translate(sirSession: SirSession): SirTranslationResult = SirTranslationResult.Untranslatable(KotlinSource(kaSymbol))

    public class Skip(symbol: KaNamedFunctionSymbol) : SirOperatorTranslationStrategy(symbol)

    public class AsIsWithAdditions(
        symbol: KaNamedFunctionSymbol,
        private val supplementaryDeclarationsProvider: (SirFunction) -> List<SirDeclaration> = { emptyList() }
    ) : SirOperatorTranslationStrategy(symbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            return SirFunctionFromKtSymbol(kaSymbol, sirSession).let {
                SirTranslationResult.OperatorFunction(it, supplementaryDeclarationsProvider(it))
            }
        }
    }

    public class AsUnaryMathOperator(symbol: KaNamedFunctionSymbol, private val name: String) : SirOperatorTranslationStrategy(symbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            return SirRenamedFunction(kaSymbol, sirSession = sirSession).let {
                SirTranslationResult.OperatorFunction(it, listOf(
                        SirUnaryMathOperatorTrampolineFunction(it, name)
                    )
                )
            }
        }
    }

    public class AsBinaryMathOperator(symbol: KaNamedFunctionSymbol, private val name: String) : SirOperatorTranslationStrategy(symbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            return SirRenamedFunction(kaSymbol, sirSession = sirSession).let {
                SirTranslationResult.OperatorFunction(it, listOf(
                        SirBinaryMathOperatorTrampolineFunction(it, name)
                    )
                )
            }
        }
    }

    public class AsCompoundAssignmentBinaryMathOperator(
        symbol: KaNamedFunctionSymbol,
        private val name: String
    ) : SirOperatorTranslationStrategy(symbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            return SirRenamedFunction(kaSymbol, sirSession = sirSession).let {
                SirTranslationResult.OperatorFunction(it, listOf(
                        SirBinaryMathOperatorTrampolineFunction(it, name)
                    )
                )
            }
        }
    }

    public class AsInvokeOperator(symbol: KaNamedFunctionSymbol): SirOperatorTranslationStrategy(symbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            return SirRenamedFunction(kaSymbol, sirSession = sirSession) { "callAsFunction" }.let {
                SirTranslationResult.OperatorFunction(it, emptyList())
            }
        }
    }

    public class AsComparisonOperator(symbol: KaNamedFunctionSymbol): SirOperatorTranslationStrategy(symbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            return SirRenamedFunction(kaSymbol, sirSession = sirSession).let {
                SirTranslationResult.OperatorFunction(it, listOf(
                        SirComparisonOperatorTrampolineFunction (it, "<"),
                        SirComparisonOperatorTrampolineFunction(it, "<="),
                        SirComparisonOperatorTrampolineFunction(it, ">"),
                        SirComparisonOperatorTrampolineFunction(it, ">="),
                    )
                )
            }
        }
    }

    public class AsSubscriptAccessor(symbol: KaNamedFunctionSymbol) : SirOperatorTranslationStrategy(symbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            if (!kaSymbol.isOperator)
                return SirTranslationResult.Untranslatable(KotlinSource(kaSymbol))

            when (kaSymbol.name.asString()) {
                "get" -> {
                    val getterFunction = SirRenamedFunction(kaSymbol, sirSession = sirSession)
                    val setterFunction: SirFunction? = sirSession.withSessions {
                        (kaSymbol.containingSymbol as? KaClassSymbol)?.declaredMemberScope?.callables
                            ?.filterIsInstance<KaNamedFunctionSymbol>()
                            ?.filter { it.isOperator && it.name.asString() == "set" }
                            ?.mapNotNull { it.toSir().primaryDeclaration as? SirFunction }
                            ?.firstOrNull {
                                it.allParameters.dropLast(1) == getterFunction.allParameters && it.allParameters.lastOrNull()?.type == getterFunction.returnType
                            }
                    }

                    return SirTranslationResult.OperatorSubscript(
                        SirSubscriptTrampoline(getterFunction, setterFunction),
                        listOf(getterFunction)
                    )
                }
                "set" -> {
                    return SirTranslationResult.OperatorFunction(
                        SirRenamedFunction(kaSymbol, sirSession),
                        emptyList()
                    )
                }
                else -> error("Unknown operator name: ${kaSymbol.name.asString()} in SirOperatorTranslationStrategy.")
            }
        }
    }
}