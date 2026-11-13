/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir

sealed interface KirAvailability {
    /**
     * The declaration is exposed to Kotlin with the given [visibility].
     */
    public data class Available(public val visibility: KirVisibility) : KirAvailability

    /**
     * The declaration can't be exposed to Kotlin.
     */
    public data class Unavailable(public val reason: String) : KirAvailability
}

val KirAvailability.visibility: KirVisibility? get() = (this as? KirAvailability.Available)?.visibility
