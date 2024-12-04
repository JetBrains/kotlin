/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.ModulesProvider
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirProvided
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities

/** A set of classes and type aliases provided by libraries (either the libraries to commonize, or their dependency libraries)/ */
sealed interface CirProvidedClassifiers {
    fun hasClassifier(classifierId: CirEntityId): Boolean
    fun classifier(classifierId: CirEntityId): CirProvided.Classifier?

    fun findTypeAliasesWithUnderlyingType(underlyingClassifier: CirEntityId): List<CirEntityId>

    object EMPTY : CirProvidedClassifiers {
        override fun hasClassifier(classifierId: CirEntityId) = false
        override fun classifier(classifierId: CirEntityId): CirProvided.Classifier? = null
        override fun findTypeAliasesWithUnderlyingType(underlyingClassifier: CirEntityId) = emptyList<CirEntityId>()
    }

    companion object {
        internal val FALLBACK_FORWARD_DECLARATION_CLASS =
            CirProvided.RegularClass(emptyList(), emptyList(), Visibilities.Public, ClassKind.CLASS)

        fun of(vararg delegates: CirProvidedClassifiers): CirProvidedClassifiers {
            val unwrappedDelegates: List<CirProvidedClassifiers> = delegates.fold(ArrayList()) { acc, delegate ->
                when (delegate) {
                    EMPTY -> Unit
                    is CompositeClassifiers -> acc.addAll(delegate.delegates)
                    else -> acc.add(delegate)
                }
                acc
            }

            return when (unwrappedDelegates.size) {
                0 -> EMPTY
                1 -> unwrappedDelegates.first()
                else -> CompositeClassifiers(unwrappedDelegates)
            }
        }

        fun by(modulesProvider: ModulesProvider?): CirProvidedClassifiers =
            if (modulesProvider != null) CirProvidedClassifiersByModules.load(modulesProvider) else EMPTY
    }
}

internal operator fun CirProvidedClassifiers.plus(other: CirProvidedClassifiers): CirProvidedClassifiers {
    return when {
        this is CompositeClassifiers && other is CompositeClassifiers -> CompositeClassifiers(this.delegates + other.delegates)
        this is CompositeClassifiers -> CompositeClassifiers(this.delegates + other)
        other is CompositeClassifiers -> CompositeClassifiers(listOf(this) + other.delegates)
        else -> CompositeClassifiers(listOf(this, other))
    }
}

private class CompositeClassifiers(val delegates: List<CirProvidedClassifiers>) : CirProvidedClassifiers {
    override fun hasClassifier(classifierId: CirEntityId) = delegates.any { it.hasClassifier(classifierId) }

    override fun classifier(classifierId: CirEntityId): CirProvided.Classifier? {
        var fallbackReturn: CirProvided.Classifier? = null
        for (delegate in delegates) {
            delegate.classifier(classifierId)?.let { classifier ->
                if (classifier !== CirProvidedClassifiers.FALLBACK_FORWARD_DECLARATION_CLASS) return classifier
                else fallbackReturn = classifier
            }
        }
        return fallbackReturn
    }

    override fun findTypeAliasesWithUnderlyingType(underlyingClassifier: CirEntityId): List<CirEntityId> {
        return delegates.flatMap { it.findTypeAliasesWithUnderlyingType(underlyingClassifier) }
    }
}
