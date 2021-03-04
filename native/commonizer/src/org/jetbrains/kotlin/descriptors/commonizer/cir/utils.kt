/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

fun <T : CirSimpleType> T.makeNullable(): T {
    if (isMarkedNullable)
        return this

    val result = when (this) {
        is CirClassType -> CirClassType.createInterned(
            classId = classifierId,
            outerType = outerType,
            visibility = visibility,
            arguments = arguments,
            isMarkedNullable = true
        )
        is CirTypeAliasType -> CirTypeAliasType.createInterned(
            typeAliasId = classifierId,
            underlyingType = underlyingType.makeNullable(),
            arguments = arguments,
            isMarkedNullable = true
        )
        is CirTypeParameterType -> CirTypeParameterType.createInterned(
            index = index,
            isMarkedNullable = true
        )
        else -> error("Unsupported type: $this")
    }

    @Suppress("UNCHECKED_CAST")
    return result as T
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : CirSimpleType> T.makeNullableIfNecessary(necessary: Boolean): T =
    if (!necessary) this else makeNullable()

fun CirClassOrTypeAliasType.unabbreviate(): CirClassType = when (this) {
    is CirClassType -> {
        var hasAbbreviationsInArguments = false
        val unabbreviatedArguments = arguments.compactMap { argument ->
            val argumentType =
                (argument as? CirTypeProjectionImpl)?.type as? CirClassOrTypeAliasType ?: return@compactMap argument
            val unabbreviatedArgumentType = argumentType.unabbreviate()

            if (argumentType == unabbreviatedArgumentType)
                argument
            else {
                hasAbbreviationsInArguments = true
                CirTypeProjectionImpl(
                    projectionKind = argument.projectionKind,
                    type = unabbreviatedArgumentType
                )
            }
        }

        val outerType = outerType
        val unabbreviatedOuterType = outerType?.unabbreviate()

        if (!hasAbbreviationsInArguments && outerType == unabbreviatedOuterType)
            this
        else
            CirClassType.createInterned(
                classId = classifierId,
                outerType = unabbreviatedOuterType,
                visibility = visibility,
                arguments = unabbreviatedArguments,
                isMarkedNullable = isMarkedNullable
            )
    }
    is CirTypeAliasType -> computeExpandedType(this).unabbreviate()
}

internal tailrec fun computeExpandedType(underlyingType: CirClassOrTypeAliasType): CirClassType {
    return when (underlyingType) {
        is CirClassType -> underlyingType
        is CirTypeAliasType -> computeExpandedType(underlyingType.underlyingType)
    }
}

@Suppress("unused", "NOTHING_TO_INLINE")
internal inline fun CirDeclaration.unsupported(): Nothing = error("This method should never be called on ${this::class.java}, $this")
