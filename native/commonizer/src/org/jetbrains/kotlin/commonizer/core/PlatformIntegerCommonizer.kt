/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.PlatformWidth
import org.jetbrains.kotlin.commonizer.mergedtree.PlatformWidthIndex
import org.jetbrains.kotlin.commonizer.mergedtree.PlatformWidthIndexImpl
import org.jetbrains.kotlin.descriptors.konan.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.SmartList

class PlatformIntegerCommonizer(
    private val typeArgumentListCommonizer: TypeArgumentListCommonizer,
    private val classifiers: CirKnownClassifiers,
) : NullableSingleInvocationCommonizer<CirClassOrTypeAliasType> {
    constructor(typeCommonizer: TypeCommonizer, classifiers: CirKnownClassifiers)
            : this(TypeArgumentListCommonizer(typeCommonizer), classifiers)

    private val platformWidthIndex: PlatformWidthIndex
        get() = PlatformWidthIndexImpl

    override fun invoke(values: List<CirClassOrTypeAliasType>): CirClassOrTypeAliasType? {
        val typesToCommonizeWithTargets = values.zip(classifiers.classifierIndices.targets)

        for ((platformTypeGroup, commonizer) in platformTypeGroupsToCommonizers) {
            // all types should belong to the same category
            if (values.any { it.classifierId !in platformTypeGroup })
                continue

            // bit width of types should match their platform width
            if (typesToCommonizeWithTargets.any { (type, target) ->
                    platformTypeGroup[type.classifierId] != platformWidthIndex.platformWidthOf(target)
                }) continue

            return commonizer(values)
        }

        return null
    }

    private val platformTypeGroupsToCommonizers: List<Pair<PlatformTypeGroup, PlatformTypeCommonizer>> = listOf(
        commonizableSignedIntegerIds to { platformIntType },
        commonizableUnsignedIntegerIds to { platformUIntType },
        commonizableSignedArrayIds to { platformIntArrayType },
        commonizableUnsignedArrayIds to { platformUIntArrayType },
        commonizableSignedRangeIds to { platformIntRangeType },
        commonizableUnsignedRangeIds to { platformUIntRangeType },
        commonizableSignedProgressionIds to { platformIntProgressionType },
        commonizableUnsignedProgressionIds to { platformUIntProgressionType },
        commonizableSignedVarIds to { types -> commonizeVarOf(types, isSigned = true) },
        commonizableUnsignedVarIds to { types -> commonizeVarOf(types, isSigned = false) },
    )

    private fun commonizeVarOf(
        types: List<CirClassOrTypeAliasType>,
        isSigned: Boolean
    ): CirClassOrTypeAliasType? {
        val commonTypeArgument = typeArgumentListCommonizer.commonize(types.map { it.arguments })?.singleOrNull()
            ?: return null

        return createCommonVarOfType(isSigned = isSigned, argument = commonTypeArgument)
    }
}

private typealias PlatformTypeCommonizer = (List<CirClassOrTypeAliasType>) -> CirClassOrTypeAliasType?
private typealias PlatformTypeGroup = Map<CirEntityId, PlatformWidth>

// commonizable groups
private val commonizableSignedIntegerIds: Map<CirEntityId, PlatformWidth> = mapOf(
    KOTLIN_INT_ID.toCirEntityId() to PlatformWidth.INT,
    KOTLIN_LONG_ID.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_INT_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private val commonizableUnsignedIntegerIds: Map<CirEntityId, PlatformWidth> = mapOf(
    KOTLIN_UINT_ID.toCirEntityId() to PlatformWidth.INT,
    KOTLIN_ULONG_ID.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_UINT_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private val commonizableSignedVarIds: Map<CirEntityId, PlatformWidth> = mapOf(
    INT_VAR_OF_ID.toCirEntityId() to PlatformWidth.INT,
    LONG_VAR_OF_ID.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_INT_VAR_OF_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private val commonizableUnsignedVarIds: Map<CirEntityId, PlatformWidth> = mapOf(
    UINT_VAR_OF_ID.toCirEntityId() to PlatformWidth.INT,
    ULONG_VAR_OF_ID.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_UINT_VAR_OF_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private val commonizableSignedArrayIds: Map<CirEntityId, PlatformWidth> = mapOf(
    INT_ARRAY_ID.toCirEntityId() to PlatformWidth.INT,
    LONG_ARRAY_ID.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_INT_ARRAY_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private val commonizableUnsignedArrayIds: Map<CirEntityId, PlatformWidth> = mapOf(
    UINT_ARRAY_ID.toCirEntityId() to PlatformWidth.INT,
    ULONG_ARRAY_ID.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_UINT_ARRAY_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private val commonizableSignedRangeIds: Map<CirEntityId, PlatformWidth> = mapOf(
    INT_RANGE_ID.toCirEntityId() to PlatformWidth.INT,
    LONG_RANGE_ID.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_INT_RANGE_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private val commonizableUnsignedRangeIds: Map<CirEntityId, PlatformWidth> = mapOf(
    UINT_RANGE_ID.toCirEntityId() to PlatformWidth.INT,
    ULONG_RANGE_ID.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_UINT_RANGE_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private val commonizableSignedProgressionIds: Map<CirEntityId, PlatformWidth> = mapOf(
    INT_PROGRESSION_ID.toCirEntityId() to PlatformWidth.INT,
    LONG_PROGRESSION_ID.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_INT_PROGRESSION_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private val commonizableUnsignedProgressionIds: Map<CirEntityId, PlatformWidth> = mapOf(
    UINT_PROGRESSION.toCirEntityId() to PlatformWidth.INT,
    ULONG_PROGRESSION.toCirEntityId() to PlatformWidth.LONG,
    PLATFORM_UINT_PROGRESSION_ID.toCirEntityId() to PlatformWidth.MIXED,
)

private fun ClassId.toCirEntityId(): CirEntityId =
    CirEntityId.create(this)

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
