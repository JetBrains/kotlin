/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.resolution

import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.ExtensionEffect
import org.jetbrains.kotlin.contracts.model.visitors.ExtensionReducer
import org.jetbrains.kotlin.contracts.model.visitors.Reducer

class ContextualEffectReducer(private val reducer: Reducer) : ExtensionReducer {
    override fun reduce(effect: ExtensionEffect): ExtensionEffect = when(effect) {
        is ContextProviderEffect -> {
            val references = effect.references
            val reducedReferences = references.map { it?.accept(reducer) as? ESValue }
            ContextProviderEffect(effect.providerDeclaration, reducedReferences, effect.owner)
        }

        is ContextVerifierEffect -> {
            val references = effect.references
            val reducedReferences = references.map { it?.accept(reducer) as? ESValue }
            ContextVerifierEffect(effect.verifierDeclaration, reducedReferences, effect.owner)
        }

        is ContextCleanerEffect -> {
            val references = effect.references
            val reducedReferences = references.map { it?.accept(reducer) as? ESValue }
            ContextCleanerEffect(effect.cleanerDeclaration, reducedReferences, effect.owner)
        }

        else -> effect
    }
}