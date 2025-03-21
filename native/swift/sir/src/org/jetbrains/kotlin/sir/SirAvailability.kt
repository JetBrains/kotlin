/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface SirAvailability {
    /**
     * Available
     * Indicates that the declaration can be made available with given visibility
     * @property visibility of resulting SirDeclaration
     */
    class Available(val visibility: SirVisibility) : SirAvailability

    /**
     * Hidden.
     * Indicates that the declatation can only be made represented in an incomplete form (e.g. with a stub)
     */
    class Hidden(val reason: String): SirAvailability

    /**
     * Unavailable.
     * Indicates that the declatation can not be possibly represented
     */
    class Unavailable(val reason: String): SirAvailability
}

val SirAvailability.visibility: SirVisibility? get() = (this as? SirAvailability.Available)?.visibility