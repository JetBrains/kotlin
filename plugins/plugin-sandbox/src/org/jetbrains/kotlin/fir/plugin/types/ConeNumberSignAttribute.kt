/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.types

import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.fir.types.ConeAttributes
import kotlin.reflect.KClass

class ConeNumberSignAttribute private constructor(val sign: Sign) : ConeAttribute<ConeNumberSignAttribute>() {
    companion object {
        private val Positive = ConeNumberSignAttribute(Sign.Positive)
        private val Negative = ConeNumberSignAttribute(Sign.Negative)

        fun fromSign(sign: Sign?): ConeNumberSignAttribute? {
            return when (sign) {
                Sign.Positive -> Positive
                Sign.Negative -> Negative
                null -> null
            }
        }
    }

    enum class Sign {
        Positive {
            override fun combine(other: Sign?): Sign? = when (other) {
                Positive -> Positive
                Negative,
                null -> null
            }
        },
        Negative {
            override fun combine(other: Sign?): Sign? = when (other) {
                Negative -> Negative
                Positive,
                null -> null
            }
        };

        abstract fun combine(other: Sign?): Sign?
    }

    private fun combine(other: ConeNumberSignAttribute?): ConeNumberSignAttribute? {
        return fromSign(sign.combine(other?.sign))
    }

    override fun union(other: ConeNumberSignAttribute?): ConeNumberSignAttribute? {
        return combine(other)
    }

    override fun intersect(other: ConeNumberSignAttribute?): ConeNumberSignAttribute? {
        return combine(other)
    }

    override fun add(other: ConeNumberSignAttribute?): ConeNumberSignAttribute? {
        return combine(other)
    }

    override fun isSubtypeOf(other: ConeNumberSignAttribute?): Boolean {
        return true
    }

    override fun toString(): String {
        return "@${sign.name}"
    }

    override val key: KClass<out ConeNumberSignAttribute>
        get() = ConeNumberSignAttribute::class
    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.numberSign: ConeNumberSignAttribute? by ConeAttributes.attributeAccessor<ConeNumberSignAttribute>()
