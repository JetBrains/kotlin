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
        is CirProvided.ClassType -> type.toCirClassTypeOrNull()
        is CirProvided.TypeAliasType -> toCirTypeAliasTypeOrNull(type)
    }
}

internal fun CirProvidedClassifiers.toCirTypeAliasTypeOrNull(type: CirProvided.TypeAliasType): CirTypeAliasType? {
    val typeAlias = this.classifier(type.classifierId) as? CirProvided.TypeAlias ?: return null
    return CirTypeAliasType.createInterned(
        typeAliasId = type.classifierId,
        isMarkedNullable = type.isMarkedNullable,
        arguments = type.arguments.map { it.toCirTypeProjection() ?: return null },
        underlyingType = toCirClassOrTypeAliasTypeOrNull(typeAlias.underlyingType) ?: return null
    )
}

internal fun CirProvided.ClassType.toCirClassTypeOrNull(): CirClassType? {
    return CirClassType.createInterned(
        classId = this.classifierId,
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