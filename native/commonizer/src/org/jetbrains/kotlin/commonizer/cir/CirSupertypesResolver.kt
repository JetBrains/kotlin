/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.findClass

internal interface CirSupertypesResolver {
    /**
     * Resolves all *declared* supertypes (not their transitive closure)
     */
    fun supertypes(type: CirClassType): Set<CirClassType>
}

/**
 * Very simple and pragmatic implementation of [CirSupertypesResolver]
 * Limitations:
 * - Will not resolve parameterized types
 * - Supertypes from dependencies are resolved in a "best effort" manner.
 */
internal class SimpleCirSupertypesResolver(
    private val classifiers: CirClassifierIndex,
    private val dependencies: CirProvidedClassifiers,
) : CirSupertypesResolver {

    override fun supertypes(type: CirClassType): Set<CirClassType> {
        classifiers.findClass(type.classifierId)?.let { classifier ->
            return supertypesFromCirClass(type, classifier)
        }

        dependencies.classifier(type.classifierId)?.let { classifier ->
            if (classifier is CirProvided.Class) {
                return dependencies.supertypesFromProvidedClass(type, classifier)
            }
        }
        return emptySet()
    }

    private fun supertypesFromCirClass(type: CirClassType, classifier: CirClass): Set<CirClassType> {
        return classifier.supertypes.filterIsInstance<CirClassType>()
            .mapNotNull { superType -> buildSupertypeFromClassifierSupertype(type, superType) }
            .toSet()
    }

    private fun CirProvidedClassifiers.supertypesFromProvidedClass(type: CirClassType, classifier: CirProvided.Class): Set<CirClassType> {
        return classifier.supertypes.filterIsInstance<CirProvided.ClassType>()
            .mapNotNull { superType -> superType.toCirClassTypeOrNull(this) }
            .mapNotNull { superType -> buildSupertypeFromClassifierSupertype(type, superType) }
            .toSet()
    }

    private fun buildSupertypeFromClassifierSupertype(type: CirClassType, supertype: CirClassType): CirClassType? {
        if (type.arguments.isEmpty() && supertype.arguments.isEmpty()) {
            return supertype.makeNullableIfNecessary(type.isMarkedNullable)
        }

        return null
    }
}
