/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

public enum class SirTypeVariance {
    /** Covariant position - type appears as output (return types, out generics) */
    COVARIANT,

    /** Contravariant position - type appears as input (parameters, in generics) */
    CONTRAVARIANT,

    /** Invariant position - type appears in both input and output (mutable properties) */
    INVARIANT;

    fun flip(): SirTypeVariance = when (this) {
        COVARIANT -> CONTRAVARIANT
        CONTRAVARIANT -> COVARIANT
        INVARIANT -> INVARIANT
    }
}
