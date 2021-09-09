/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClassOrTypeAliasType
import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.cir.CirTypeAliasType
import org.jetbrains.kotlin.commonizer.cir.CirTypeProjection
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

internal tailrec fun computeSuitableUnderlyingType(
    classifiers: CirKnownClassifiers,
    typeCommonizer: TypeCommonizer,
    underlyingType: CirClassOrTypeAliasType
): CirClassOrTypeAliasType? {
    return when (underlyingType) {
        is CirClassType -> underlyingType.withCommonizedArguments(typeCommonizer)
        is CirTypeAliasType ->
            if (classifiers.commonDependencies.hasClassifier(underlyingType.classifierId) ||
                classifiers.commonizedNodes.typeAliasNode(underlyingType.classifierId)?.commonDeclaration?.invoke() != null
            ) underlyingType.withCommonizedArguments(typeCommonizer)
            else computeSuitableUnderlyingType(classifiers, typeCommonizer, underlyingType.underlyingType)
    }
}

private fun CirClassType.withCommonizedArguments(typeCommonizer: TypeCommonizer): CirClassType? {
    val existingArguments = arguments
    val newArguments = existingArguments.toCommonizedArguments(typeCommonizer) ?: return null

    val existingOuterType = outerType
    val newOuterType = existingOuterType?.let { it.withCommonizedArguments(typeCommonizer) ?: return null }

    return if (newArguments !== existingArguments || newOuterType !== existingOuterType)
        CirClassType.createInterned(
            classId = classifierId,
            outerType = newOuterType,
            visibility = visibility,
            arguments = newArguments,
            isMarkedNullable = isMarkedNullable
        )
    else
        this
}

private fun CirTypeAliasType.withCommonizedArguments(typeCommonizer: TypeCommonizer): CirTypeAliasType? {
    val existingArguments = arguments
    val newArguments = existingArguments.toCommonizedArguments(typeCommonizer) ?: return null

    val existingUnderlyingType = underlyingType
    val newUnderlyingType = when (existingUnderlyingType) {
        is CirClassType -> existingUnderlyingType.withCommonizedArguments(typeCommonizer)
        is CirTypeAliasType -> existingUnderlyingType.withCommonizedArguments(typeCommonizer)
    } ?: return null

    return if (newArguments !== existingArguments || newUnderlyingType !== existingUnderlyingType)
        CirTypeAliasType.createInterned(
            typeAliasId = classifierId,
            underlyingType = newUnderlyingType,
            arguments = newArguments,
            isMarkedNullable = isMarkedNullable
        )
    else
        this
}

@Suppress("NOTHING_TO_INLINE")
private inline fun List<CirTypeProjection>.toCommonizedArguments(typeCommonizer: TypeCommonizer): List<CirTypeProjection>? =
    if (isEmpty()) this
    else TypeArgumentListCommonizer(typeCommonizer).let { if (it.commonizeWith(this)) it.result else null }
