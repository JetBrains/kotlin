/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual

import org.jetbrains.kotlin.contracts.contextual.model.ContextCleaner
import org.jetbrains.kotlin.contracts.contextual.model.ContextProvider
import org.jetbrains.kotlin.contracts.contextual.model.ContextVerifier
import org.jetbrains.kotlin.extensions.ExtensionBindingContextData

data class FactsBindingInfo(
    val providers: Collection<ContextProvider> = listOf(),
    val verifiers: Collection<ContextVerifier> = listOf(),
    val cleaners: Collection<ContextCleaner> = listOf()
) : ExtensionBindingContextData {
    companion object {
        val EMPTY = FactsBindingInfo()
    }

    constructor(provider: ContextProvider) : this(providers = listOf(provider))

    constructor(verifier: ContextVerifier) : this(verifiers = listOf(verifier))

    constructor(cleaner: ContextCleaner) : this(cleaners = listOf(cleaner))

    fun isEmpty(): Boolean = providers.isEmpty() && verifiers.isEmpty() && cleaners.isEmpty()

    override fun combine(other: ExtensionBindingContextData): ExtensionBindingContextData {
        if (other !is FactsBindingInfo) {
            error("Incompatible types: FactsBindingInfo and ${other::class}")
        }
        return when {
            this.isEmpty() -> other
            other.isEmpty() -> this
            else -> FactsBindingInfo(
                providers + other.providers,
                verifiers + other.verifiers,
                cleaners + other.cleaners
            )
        }
    }
}