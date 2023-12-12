/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.formver.viper.ast.Info

sealed interface SourceRole {
    data object ParamFunctionLeakageCheck : SourceRole

    data class ListElementAccessCheck(val accessType: AccessCheckType) : SourceRole {
        enum class AccessCheckType {
            LESS_THAN_ZERO,
            GREATER_THAN_LIST_SIZE
        }
    }

    data class CallsInPlaceEffect(val paramSymbol: FirBasedSymbol<*>, val kind: EventOccurrencesRange) : SourceRole
    data class ConditionalEffect(val effect: ReturnsEffect, val condition: Condition) : SourceRole
    data class FirSymbolHolder(val firSymbol: FirBasedSymbol<*>) : SourceRole, Condition

    sealed interface ReturnsEffect : SourceRole {
        data object Wildcard : ReturnsEffect
        data class Bool(val bool: Boolean) : ReturnsEffect {
            override fun toString(): String = bool.toString()
        }

        data class Null(val negated: Boolean) : ReturnsEffect {
            override fun toString(): String = if (negated) {
                "non-null"
            } else {
                "null"
            }
        }
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
