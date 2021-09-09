/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvided
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.findClass
import org.jetbrains.kotlin.descriptors.Visibilities

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
                return supertypesFromProvidedClass(type, classifier)
            }
        }
        return emptySet()
    }

    private fun supertypesFromCirClass(type: CirClassType, classifier: CirClass): Set<CirClassType> {
        return classifier.supertypes.filterIsInstance<CirClassType>()
            .mapNotNull { superType -> buildSupertypeFromClassifierSupertype(type, superType) }
            .toSet()
    }

    private fun supertypesFromProvidedClass(type: CirClassType, classifier: CirProvided.Class): Set<CirClassType> {
        return classifier.supertypes.filterIsInstance<CirProvided.ClassType>()
            .mapNotNull { superType -> superType.toCirClassTypeOrNull() }
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

internal fun CirProvidedClassifiers.toCirClassOrTypeAliasTypeOrNull(type: CirProvided.Type): CirClassOrTypeAliasType? {
    return when (type) {
        is CirProvided.ClassType -> type.toCirClassTypeOrNull()
        is CirProvided.TypeParameterType -> null
        is CirProvided.TypeAliasType -> TODO()
    }
}

internal fun CirProvidedClassifiers.toCirTypeAliasTypeOrNull(type: CirProvided.TypeAliasType): CirTypeAliasType? {
    val typeAlias = this.classifier(type.typeAliasId) as? CirProvided.TypeAlias ?: return null
    return CirTypeAliasType.createInterned(
        typeAliasId = type.typeAliasId,
        isMarkedNullable = type.isMarkedNullable,
        arguments = type.arguments.map { it.toCirTypeProjection() ?: return null },
        underlyingType = toCirClassOrTypeAliasTypeOrNull(typeAlias.underlyingType) ?: return null
    )
}

// TODO NOW move
internal fun CirProvided.ClassType.toCirClassTypeOrNull(): CirClassType? {
    return CirClassType.createInterned(
        classId = this.classId,
        outerType = this.outerType?.let { it.toCirClassTypeOrNull() ?: return null },
        isMarkedNullable = this.isMarkedNullable,
        arguments = this.arguments.map { it.toCirTypeProjection() ?: return null },
        visibility = Visibilities.Public,
    )
}

internal fun CirProvided.TypeProjection.toCirTypeProjection(): CirTypeProjection? {
    return when (this) {
        is CirProvided.StarTypeProjection -> CirStarTypeProjection
        is CirProvided.RegularTypeProjection -> CirRegularTypeProjection(
            projectionKind = variance,
            type = (type as? CirProvided.ClassType)?.toCirClassTypeOrNull() ?: return null
        )
    }
}