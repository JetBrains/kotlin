/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.viper.ast.Info
import org.jetbrains.kotlin.formver.viper.ast.unwrap
import org.jetbrains.kotlin.formver.viper.errors.ErrorReason
import org.jetbrains.kotlin.formver.viper.errors.extractInfoFromFunctionArgument

sealed interface SourceRole {
    data object ParamFunctionLeakageCheck : SourceRole {
        /**
         * Retrieves the leaking function parameter symbol from an error reason.
         * This method is specifically used for identifying the function parameter that violates the `callsInPlace` contract.
         */
        fun ErrorReason.fetchLeakingFunction(): FirBasedSymbol<*> =
            extractInfoFromFunctionArgument(0).unwrap<FirSymbolHolder>().firSymbol
    }

    data class CallsInPlaceEffect(val paramSymbol: FirBasedSymbol<*>, val kind: EventOccurrencesRange) : SourceRole
    data class ConditionalEffect(val effect: ReturnsEffect, val condition: Condition) : SourceRole
    data class FirSymbolHolder(val firSymbol: FirBasedSymbol<*>) : SourceRole, Condition

    sealed interface ReturnsEffect : SourceRole {
        data object Wildcard : ReturnsEffect
        data class Bool(val bool: Boolean) : ReturnsEffect
        data class Null(val negated: Boolean) : ReturnsEffect
    }

    sealed interface Condition : SourceRole {
        data class IsType(val targetVariable: FirBasedSymbol<*>, val expectedType: ConeKotlinType, val negated: Boolean = false) : Condition
        data class IsNull(val targetVariable: FirBasedSymbol<*>, val negated: Boolean = false) : Condition
        data class Constant(val literal: Boolean) : Condition
        data class Conjunction(val lhs: Condition, val rhs: Condition) : Condition
        data class Disjunction(val lhs: Condition, val rhs: Condition) : Condition
        data class Negation(val arg: Condition) : Condition
    }
}



val SourceRole?.asInfo: Info
    get() = when (this) {
        null -> Info.NoInfo
        else -> Info.Wrapped(this)
    }
