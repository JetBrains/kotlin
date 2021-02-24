/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId

/** A set of classes and type aliases provided by libraries (either the libraries to commonize, or their dependency libraries)/ */
interface CirProvidedClassifiers {
    fun hasClassifier(classifierId: CirEntityId): Boolean

    // TODO: implement later
    //fun classifier(classifierId: ClassId): Any?

    object EMPTY : CirProvidedClassifiers {
        override fun hasClassifier(classifierId: CirEntityId) = false
    }

    private class CompositeClassifiers(val delegates: List<CirProvidedClassifiers>) : CirProvidedClassifiers {
        override fun hasClassifier(classifierId: CirEntityId) = delegates.any { it.hasClassifier(classifierId) }
    }

    companion object {
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
            if (modulesProvider != null) CirProvidedClassifiersByModules(modulesProvider) else EMPTY
    }
}
