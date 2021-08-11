/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvided
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.findClass
import org.jetbrains.kotlin.commonizer.util.transitiveClosure
import org.jetbrains.kotlin.descriptors.Visibilities

internal interface CirSupertypesResolver {
    fun supertypes(type: CirClassType): Set<CirClassType>
}

internal fun CirSupertypesResolver.allSupertypes(type: CirClassType): Set<CirClassType> {
    return transitiveClosure(type, this::supertypes)
}

internal class SimpleCirSupertypesResolver(
    private val classifiers: CirClassifierIndex,
    private val dependencies: CirProvidedClassifiers,
) : CirSupertypesResolver {

    override fun supertypes(type: CirClassType): Set<CirClassType> {
        classifiers.findClass(type.classifierId)?.let { classifier ->
            return supertypes(type, classifier)
        }

        dependencies.classifier(type.classifierId)?.let { classifier ->
            if (classifier is CirProvided.Class) {
                return supertypes(type, classifier)
            }
        }
        return emptySet()
    }

    private fun supertypes(type: CirClassType, classifier: CirClass): Set<CirClassType> {
        return classifier.supertypes.filterIsInstance<CirClassType>()
            .mapNotNull { superType -> createSupertype(type, superType) }
            .toSet()
    }

    private fun supertypes(type: CirClassType, classifier: CirProvided.Class): Set<CirClassType> {
        return classifier.supertypes.filterIsInstance<CirProvided.ClassType>()
            .mapNotNull { superType -> superType.toCirClassTypeOrNull() }
            .mapNotNull { superType -> createSupertype(type, superType) }
            .toSet()
    }

    private fun createSupertype(type: CirClassType, supertype: CirClassType): CirClassType? {
        if (type.arguments.isEmpty() && supertype.arguments.isEmpty()) {
            return supertype
        }

        return null
    }
}

private fun CirProvided.ClassType.toCirClassTypeOrNull(): CirClassType? {
    return CirClassType.createInterned(
        classId = this.classId,
        outerType = this.outerType?.let { it.toCirClassTypeOrNull() ?: return null },
        isMarkedNullable = this.isMarkedNullable,
        arguments = this.arguments.map { it.toCirTypeProjection() ?: return null },
        visibility = Visibilities.Public,
    )
}

private fun CirProvided.TypeProjection.toCirTypeProjection(): CirTypeProjection? {
    return when (this) {
        is CirProvided.StarTypeProjection -> CirStarTypeProjection
        is CirProvided.RegularTypeProjection -> CirRegularTypeProjection(
            projectionKind = variance,
            type = (type as? CirProvided.ClassType)?.toCirClassTypeOrNull() ?: return null
        )
    }
}