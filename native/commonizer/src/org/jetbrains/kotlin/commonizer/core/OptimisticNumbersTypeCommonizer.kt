/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.cir.CirClassType.Companion.copyInterned
import org.jetbrains.kotlin.commonizer.utils.*

private typealias BitWidth = Int

private class SubstitutableNumbers(val numbers: Map<CirEntityId, BitWidth>) {
    operator fun contains(id: CirEntityId) = id in numbers
    fun choose(first: CirClassType, second: CirClassType): CirClassType? {
        val firstBitWidth = numbers[first.classifierId] ?: return null
        val secondBitWidth = numbers[second.classifierId] ?: return null
        return if (secondBitWidth < firstBitWidth) second else first
    }
}

private val signedIntegers = SubstitutableNumbers(
    mapOf(
        CirEntityId.create(KOTLIN_BYTE_ID) to 8,
        CirEntityId.create(KOTLIN_SHORT_ID) to 16,
        CirEntityId.create(KOTLIN_INT_ID) to 32,
        CirEntityId.create(KOTLIN_LONG_ID) to 64
    )
)

private val unsignedIntegers = SubstitutableNumbers(
    mapOf(
        CirEntityId.create(KOTLIN_UBYTE_ID) to 8,
        CirEntityId.create(KOTLIN_USHORT_ID) to 16,
        CirEntityId.create(KOTLIN_UINT_ID) to 32,
        CirEntityId.create(KOTLIN_ULONG_ID) to 64
    )
)

private val floatingPoints = SubstitutableNumbers(
    mapOf(
        CirEntityId.create(KOTLIN_FLOAT_ID) to 32,
        CirEntityId.create(KOTLIN_DOUBLE_ID) to 64,
    )
)

private val signedVarIntegers = SubstitutableNumbers(
    mapOf(
        CirEntityId.create(BYTE_VAR_OF_ID) to 8,
        CirEntityId.create(SHORT_VAR_OF_ID) to 16,
        CirEntityId.create(INT_VAR_OF_ID) to 32,
        CirEntityId.create(LONG_VAR_OF_ID) to 64,
    )
)

private val unsignedVarIntegers = SubstitutableNumbers(
    mapOf(
        CirEntityId.create(UBYTE_VAR_OF_ID) to 8,
        CirEntityId.create(USHORT_VAR_OF_ID) to 16,
        CirEntityId.create(UINT_VAR_OF_ID) to 32,
        CirEntityId.create(ULONG_VAR_OF_ID) to 64,
    )
)

private val floatingPointVars = SubstitutableNumbers(
    mapOf(
        CirEntityId.create(FLOAT_VAR_OF_ID) to 32,
        CirEntityId.create(DOUBLE_VAR_OF_ID) to 64,
    )
)

internal object OptimisticNumbersTypeCommonizer : AssociativeCommonizer<CirClassType> {
    private val commonizableNumberTypes = listOf(
        signedIntegers,
        unsignedIntegers,
        floatingPoints,
        signedVarIntegers,
        unsignedVarIntegers,
        floatingPointVars,
    )

    private val commonizableNumberIdentifiers = commonizableNumberTypes.flatMap { it.numbers.keys }.toSet()

    fun isOptimisticallyCommonizableNumber(identifier: CirEntityId): Boolean = identifier in commonizableNumberIdentifiers

    override fun commonize(first: CirClassType, second: CirClassType): CirClassType? {
        val result = commonizableNumberTypes.firstNotNullOfOrNull { it.choose(first, second) }
        return result?.withMarker()
    }

    private fun CirClassType.withMarker(): CirClassType = this.copyInterned(
        attachments = attachments + OptimisticCommonizationMarker
    )

    internal object OptimisticCommonizationMarker : CirTypeAttachment
}