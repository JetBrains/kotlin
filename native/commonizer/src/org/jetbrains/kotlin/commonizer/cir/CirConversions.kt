/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.mergedtree.CirProvidedClassifiers

internal fun CirProvided.ClassOrTypeAliasType.toCirClassOrTypeAliasTypeOrNull(classifiers: CirProvidedClassifiers): CirClassOrTypeAliasType? {
    return when (this) {
        is CirProvided.ClassType -> this.toCirClassTypeOrNull(classifiers)
        is CirProvided.TypeAliasType -> this.toCirTypeAliasTypeOrNull(classifiers)
    }
}

internal fun CirProvided.TypeAliasType.toCirTypeAliasTypeOrNull(classifiers: CirProvidedClassifiers): CirTypeAliasType? {
    val typeAlias = classifiers.classifier(classifierId) as? CirProvided.TypeAlias ?: return null
    return CirTypeAliasType.createInterned(
        typeAliasId = classifierId,
        isMarkedNullable = isMarkedNullable,
        arguments = arguments.map { it.toCirTypeProjectionOrNull(classifiers) ?: return null },
        underlyingType = typeAlias.underlyingType.toCirClassOrTypeAliasTypeOrNull(classifiers) ?: return null
    )
}

internal fun CirProvided.ClassType.toCirClassTypeOrNull(classifiers: CirProvidedClassifiers): CirClassType? {
    return CirClassType.createInterned(
        classId = classifierId,
        outerType = outerType?.let { it.toCirClassTypeOrNull(classifiers) ?: return null },
        isMarkedNullable = isMarkedNullable,
        arguments = arguments.map { it.toCirTypeProjectionOrNull(classifiers) ?: return null },
    )
}

internal fun CirProvided.TypeProjection.toCirTypeProjectionOrNull(classifiers: CirProvidedClassifiers): CirTypeProjection? {
    return when (this) {
        is CirProvided.StarTypeProjection -> CirStarTypeProjection
        is CirProvided.RegularTypeProjection -> this.toCirRegularTypeProjectionOrNull(classifiers)
    }
}

internal fun CirProvided.RegularTypeProjection.toCirRegularTypeProjectionOrNull(
    classifiers: CirProvidedClassifiers
): CirRegularTypeProjection? {
    return CirRegularTypeProjection(
        projectionKind = variance,
        type = when (val type = type) {
            is CirProvided.ClassOrTypeAliasType -> type.toCirClassOrTypeAliasTypeOrNull(classifiers) ?: return null
            is CirProvided.TypeParameterType -> CirTypeParameterType.createInterned(type.index, type.isMarkedNullable)
        }
    )
}