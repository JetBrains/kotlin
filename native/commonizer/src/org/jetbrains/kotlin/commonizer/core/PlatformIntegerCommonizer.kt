/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.konan.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.SmartList

class PlatformIntegerCommonizer(
    private val typeArgumentListCommonizer: TypeArgumentListCommonizer
) : AssociativeCommonizer<CirClassOrTypeAliasType> {
    constructor(typeCommonizer: TypeCommonizer) : this(TypeArgumentListCommonizer(typeCommonizer))

    override fun commonize(first: CirClassOrTypeAliasType, second: CirClassOrTypeAliasType): CirClassOrTypeAliasType? {
        return when {
            both(first, second) { it.classifierId in commonizableSignedIntegerIds } -> platformIntType
            both(first, second) { it.classifierId in commonizableUnsignedIntegerIds } -> platformUIntType
            both(first, second) { it.classifierId in commonizableSignedVarIds } -> commonizeVarOf(first, second, isSigned = true)
            both(first, second) { it.classifierId in commonizableUnsignedVarIds } -> commonizeVarOf(first, second, isSigned = false)
            both(first, second) { it.classifierId in commonizableSignedArrayIds } -> platformIntArrayType
            both(first, second) { it.classifierId in commonizableUnsignedArrayIds } -> platformUIntArrayType
            both(first, second) { it.classifierId in commonizableSignedRangeIds } -> platformIntRangeType
            both(first, second) { it.classifierId in commonizableUnsignedRangeIds } -> platformUIntRangeType
            both(first, second) { it.classifierId in commonizableSignedProgressionIds } -> platformIntProgressionType
            both(first, second) { it.classifierId in commonizableUnsignedProgressionIds } -> platformUIntProgressionType
            else -> null
        }
    }
    
    private inline fun <T> both(first: T, second: T, predicate: (T) -> Boolean) = 
        predicate(first) && predicate(second)

    private fun commonizeVarOf(
        first: CirClassOrTypeAliasType,
        second: CirClassOrTypeAliasType,
        isSigned: Boolean
    ): CirClassOrTypeAliasType? {
        if (first !is CirClassType || second !is CirClassType) return null

        val argument = typeArgumentListCommonizer.commonize(listOf(first.arguments, second.arguments))?.singleOrNull()
            ?: return null

        return createCommonVarOfType(isSigned = isSigned, argument = argument)
    }
}

// commonizable groups
private val commonizableSignedIntegerIds: Set<CirEntityId> = listOf(
    KOTLIN_INT_ID, KOTLIN_LONG_ID, PLATFORM_INT_ID
).toCirEntityIds()

private val commonizableUnsignedIntegerIds: Set<CirEntityId> = listOf(
    KOTLIN_UINT_ID, KOTLIN_ULONG_ID, PLATFORM_UINT_ID
).toCirEntityIds()

private val commonizableSignedVarIds: Set<CirEntityId> = listOf(
    INT_VAR_OF_ID, LONG_VAR_OF_ID, PLATFORM_INT_VAR_OF_ID
).toCirEntityIds()

private val commonizableUnsignedVarIds: Set<CirEntityId> = listOf(
    UINT_VAR_OF_ID, ULONG_VAR_OF_ID, PLATFORM_UINT_VAR_OF_ID
).toCirEntityIds()

private val commonizableSignedArrayIds: Set<CirEntityId> = listOf(
    INT_ARRAY_ID, LONG_ARRAY_ID, PLATFORM_INT_ARRAY_ID
).toCirEntityIds()

private val commonizableUnsignedArrayIds: Set<CirEntityId> = listOf(
    UINT_ARRAY_ID, ULONG_ARRAY_ID, PLATFORM_UINT_ARRAY_ID
).toCirEntityIds()

private val commonizableSignedRangeIds: Set<CirEntityId> = listOf(
    INT_RANGE_ID, LONG_RANGE_ID, PLATFORM_INT_RANGE_ID
).toCirEntityIds()

private val commonizableUnsignedRangeIds: Set<CirEntityId> = listOf(
    UINT_RANGE_ID, ULONG_RANGE_ID, PLATFORM_UINT_RANGE_ID
).toCirEntityIds()

private val commonizableSignedProgressionIds: Set<CirEntityId> = listOf(
    INT_PROGRESSION_ID, LONG_PROGRESSION_ID, PLATFORM_INT_PROGRESSION_ID
).toCirEntityIds()

private val commonizableUnsignedProgressionIds: Set<CirEntityId> = listOf(
    UINT_PROGRESSION, ULONG_PROGRESSION, PLATFORM_UINT_PROGRESSION_ID
).toCirEntityIds()

private fun ClassId.toCirEntityId(): CirEntityId =
    CirEntityId.create(this)

private fun Collection<ClassId>.toCirEntityIds(): Set<CirEntityId> =
    map { it.toCirEntityId()}.toSet()

// plain integers
private val platformIntType: CirClassType = createSimpleCirTypeWithoutArguments(PLATFORM_INT_ID.toCirEntityId())
private val platformUIntType: CirClassType = createSimpleCirTypeWithoutArguments(PLATFORM_UINT_ID.toCirEntityId())

// arrays
private val platformIntArrayType: CirClassType = createSimpleCirTypeWithoutArguments(PLATFORM_INT_ARRAY_ID.toCirEntityId())
private val platformUIntArrayType: CirClassType = createSimpleCirTypeWithoutArguments(PLATFORM_UINT_ARRAY_ID.toCirEntityId())

// ranges
private val platformIntRangeType: CirClassType = createSimpleCirTypeWithoutArguments(PLATFORM_INT_RANGE_ID.toCirEntityId())
private val platformUIntRangeType: CirClassType = createSimpleCirTypeWithoutArguments(PLATFORM_UINT_RANGE_ID.toCirEntityId())

// progressions
private val platformIntProgressionType: CirClassType = createSimpleCirTypeWithoutArguments(PLATFORM_INT_PROGRESSION_ID.toCirEntityId())
private val platformUIntProgressionType: CirClassType = createSimpleCirTypeWithoutArguments(PLATFORM_UINT_PROGRESSION_ID.toCirEntityId())

private fun createSimpleCirTypeWithoutArguments(id: CirEntityId): CirClassType =
    CirClassType.createInterned(
        classId = id,
        outerType = null,
        arguments = emptyList(),
        isMarkedNullable = false,
    )

private fun createCommonVarOfType(isSigned: Boolean, argument: CirTypeProjection): CirClassType {
    return CirClassType.createInterned(
        classId = if (isSigned) PLATFORM_INT_VAR_OF_ID.toCirEntityId() else PLATFORM_UINT_VAR_OF_ID.toCirEntityId(),
        outerType = null,
        arguments = SmartList(argument),
        isMarkedNullable = false,
    )
}
