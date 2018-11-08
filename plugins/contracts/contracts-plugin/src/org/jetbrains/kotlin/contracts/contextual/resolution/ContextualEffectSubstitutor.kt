/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.resolution

import org.jetbrains.kotlin.contracts.model.ESEffect
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.ExtensionEffect
import org.jetbrains.kotlin.contracts.model.structure.ESFunction
import org.jetbrains.kotlin.contracts.model.visitors.Substitutor

fun substituteContextualEffect(effect: ExtensionEffect, substitutor: Substitutor): ESEffect? {
    return when(effect) {
        is ContextProviderEffect -> {
            val substitutionForCallable = if (effect.owner is ESFunction)
                effect.owner
            else
                effect.owner.accept(substitutor) as? ESValue ?: return null
            val substitutedReferences = effect.references.map { it?.accept(substitutor) as? ESValue }
            ContextProviderEffect(effect.providerDeclaration, substitutedReferences, substitutionForCallable)
        }

        is ContextVerifierEffect -> {
            val substitutionForCallable = if (effect.owner is ESFunction)
                effect.owner
            else
                effect.owner.accept(substitutor) as? ESValue ?: return null
            val substitutedReferences = effect.references.map { it?.accept(substitutor) as? ESValue }
            ContextVerifierEffect(effect.verifierDeclaration, substitutedReferences, substitutionForCallable)
        }

        is ContextCleanerEffect -> {
            val substitutionForCallable = if (effect.owner is ESFunction)
                effect.owner
            else
                effect.owner.accept(substitutor) as? ESValue ?: return null
            val substitutedReferences = effect.references.map { it?.accept(substitutor) as? ESValue }
            ContextCleanerEffect(effect.cleanerDeclaration, substitutedReferences, substitutionForCallable)
        }

        else -> null
    }
}
