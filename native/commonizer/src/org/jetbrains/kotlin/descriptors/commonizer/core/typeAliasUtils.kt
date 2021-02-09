/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassOrTypeAliasType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeAliasType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeProjection
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers

tailrec fun computeExpandedType(underlyingType: CirClassOrTypeAliasType): CirClassType {
    return when (underlyingType) {
        is CirClassType -> underlyingType
        is CirTypeAliasType -> computeExpandedType(underlyingType.underlyingType)
    }
}

internal tailrec fun computeSuitableUnderlyingType(
    classifiers: CirKnownClassifiers,
    underlyingType: CirClassOrTypeAliasType
): CirClassOrTypeAliasType? {
    return when (underlyingType) {
        is CirClassType -> underlyingType.withCommonizedArguments(classifiers)
        is CirTypeAliasType -> if (classifiers.commonDependeeLibraries.hasClassifier(underlyingType.classifierId))
            underlyingType.withCommonizedArguments(classifiers)
        else
            computeSuitableUnderlyingType(classifiers, underlyingType.underlyingType)
    }
}

private fun CirClassType.withCommonizedArguments(classifiers: CirKnownClassifiers): CirClassType? {
    val existingArguments = arguments
    val newArguments = existingArguments.toCommonizedArguments(classifiers) ?: return null

    val existingOuterType = outerType
    val newOuterType = existingOuterType?.let { it.withCommonizedArguments(classifiers) ?: return null }

    return if (newArguments !== existingArguments || newOuterType !== existingOuterType)
        CirTypeFactory.createClassType(
            classId = classifierId,
            outerType = newOuterType,
            visibility = visibility,
            arguments = newArguments,
            isMarkedNullable = isMarkedNullable
        )
    else
        this
}

private fun CirTypeAliasType.withCommonizedArguments(classifiers: CirKnownClassifiers): CirTypeAliasType? {
    val existingArguments = arguments
    val newArguments = existingArguments.toCommonizedArguments(classifiers) ?: return null

    val existingUnderlyingType = underlyingType
    val newUnderlyingType = when (existingUnderlyingType) {
        is CirClassType -> existingUnderlyingType.withCommonizedArguments(classifiers)
        is CirTypeAliasType -> existingUnderlyingType.withCommonizedArguments(classifiers)
    } ?: return null

    return if (newArguments !== existingArguments || newUnderlyingType !== existingUnderlyingType)
        CirTypeFactory.createTypeAliasType(
            typeAliasId = classifierId,
            underlyingType = newUnderlyingType,
            arguments = newArguments,
            isMarkedNullable = isMarkedNullable
        )
    else
        this
}

@Suppress("NOTHING_TO_INLINE")
private inline fun List<CirTypeProjection>.toCommonizedArguments(classifiers: CirKnownClassifiers): List<CirTypeProjection>? =
    if (isEmpty())
        this
    else
        TypeArgumentListCommonizer(classifiers).let { if (it.commonizeWith(this)) it.result else null }
