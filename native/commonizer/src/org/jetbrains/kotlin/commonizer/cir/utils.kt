/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.utils.compactMap

fun <T : CirSimpleType> T.makeNullable(): T {
    if (isMarkedNullable)
        return this

    val result = when (this) {
        is CirClassType -> CirClassType.createInterned(
            classId = classifierId,
            outerType = outerType,
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
                (argument as? CirRegularTypeProjection)?.type as? CirClassOrTypeAliasType ?: return@compactMap argument
            val unabbreviatedArgumentType = argumentType.unabbreviate()

            if (argumentType == unabbreviatedArgumentType)
                argument
            else {
                hasAbbreviationsInArguments = true
                CirRegularTypeProjection(
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

internal fun CirClassOrTypeAliasType.withParentArguments(
    parentArguments: List<CirTypeProjection>
): CirClassOrTypeAliasType {
    val newArguments = arguments.map { oldArgument ->
        if (oldArgument !is CirRegularTypeProjection) return@map oldArgument

        when (val type = oldArgument.type) {
            is CirTypeParameterType -> parentArguments[type.index]
            is CirClassOrTypeAliasType -> CirRegularTypeProjection(
                oldArgument.projectionKind, type.withParentArguments(parentArguments)
            )
            else -> oldArgument
        }
    }

    return when (val newType = withArguments(newArguments)) {
        this -> this
        is CirClassType -> newType
        is CirTypeAliasType -> newType.withUnderlyingType(
            newType.underlyingType.withParentArguments(parentArguments)
        )
    }
}

internal tailrec fun CirClassOrTypeAliasType.expandedType(): CirClassType = when (this) {
    is CirClassType -> this
    is CirTypeAliasType -> this.underlyingType.expandedType()
}
