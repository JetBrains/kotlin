/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.viper.ast.Info
import org.jetbrains.kotlin.formver.viper.ast.unwrap
import org.jetbrains.kotlin.formver.viper.errors.ErrorReason
import org.jetbrains.kotlin.formver.viper.errors.extractInfoFromFunctionArgument

sealed interface SourceRole {
    data object ReturnsEffect : SourceRole
    data object ReturnsTrueEffect : SourceRole
    data object ReturnsFalseEffect : SourceRole
    data object ReturnsNullEffect : SourceRole
    data object ReturnsNotNullEffect : SourceRole
    data class CallsInPlaceEffect(val paramSymbol: FirBasedSymbol<*>, val kind: EventOccurrencesRange) : SourceRole
    data class FirSymbolHolder(val firSymbol: FirBasedSymbol<*>) : SourceRole
    data object ParamFunctionLeakageCheck : SourceRole {
        /**
         * Retrieves the leaking function parameter symbol from an error reason.
         * This method is specifically used for identifying the function parameter that violates the `callsInPlace` contract.
         */
        fun ErrorReason.fetchLeakingFunction(): FirBasedSymbol<*> =
            extractInfoFromFunctionArgument(0).unwrap<FirSymbolHolder>().firSymbol
    }
}

val SourceRole?.asInfo: Info
    get() = when (this) {
        null -> Info.NoInfo
        else -> Info.Wrapped(this)
    }
