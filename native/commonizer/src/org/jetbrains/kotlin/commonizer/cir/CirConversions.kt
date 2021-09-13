/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.mergedtree.CirProvided
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.descriptors.Visibilities

internal fun CirProvidedClassifiers.toCirClassOrTypeAliasTypeOrNull(type: CirProvided.ClassOrTypeAliasType): CirClassOrTypeAliasType? {
    return when (type) {
        is CirProvided.ClassType -> toCirClassTypeOrNull(type)
        is CirProvided.TypeAliasType -> toCirTypeAliasTypeOrNull(type)
    }
}

internal fun CirProvidedClassifiers.toCirTypeAliasTypeOrNull(type: CirProvided.TypeAliasType): CirTypeAliasType? {
    val typeAlias = this.classifier(type.classifierId) as? CirProvided.TypeAlias ?: return null
    return CirTypeAliasType.createInterned(
        typeAliasId = type.classifierId,
        isMarkedNullable = type.isMarkedNullable,
        arguments = type.arguments.map { toCirTypeProjection(it) ?: return null },
        underlyingType = toCirClassOrTypeAliasTypeOrNull(typeAlias.underlyingType) ?: return null
    )
}

internal fun CirProvidedClassifiers.toCirClassTypeOrNull(type: CirProvided.ClassType): CirClassType? {
    return CirClassType.createInterned(
        classId = type.classifierId,
        outerType = type.outerType?.let { toCirClassTypeOrNull(it) ?: return null },
        isMarkedNullable = type.isMarkedNullable,
        arguments = type.arguments.map { toCirTypeProjection(it) ?: return null },
        visibility = Visibilities.Public,
    )
}

internal fun CirProvidedClassifiers.toCirTypeProjection(type: CirProvided.TypeProjection): CirTypeProjection? {
    return when (type) {
        is CirProvided.StarTypeProjection -> CirStarTypeProjection
        is CirProvided.RegularTypeProjection -> toCirRegularTypeProjectionOrNull(type)
    }
}

internal fun CirProvidedClassifiers.toCirRegularTypeProjectionOrNull(
    projection: CirProvided.RegularTypeProjection
): CirRegularTypeProjection? {
    return CirRegularTypeProjection(
        projectionKind = projection.variance,
        type = when (val type = projection.type) {
            is CirProvided.ClassOrTypeAliasType -> toCirClassOrTypeAliasTypeOrNull(type) ?: return null
            is CirProvided.TypeParameterType -> CirTypeParameterType.createInterned(type.index, type.isMarkedNullable)
        }
    )
}