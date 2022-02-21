/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.utils.*

private typealias BitWidth = Int

private class SubstitutableNumbers(private val numbers: Map<CirEntityId, BitWidth>) {
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
    override fun commonize(first: CirClassType, second: CirClassType): CirClassType? {
        return signedIntegers.choose(first, second)
            ?: unsignedIntegers.choose(first, second)
            ?: floatingPoints.choose(first, second)
            ?: signedVarIntegers.choose(first, second)
            ?: unsignedVarIntegers.choose(first, second)
            ?: floatingPointVars.choose(first, second)
    }

    fun isOptimisticallySubstitutable(classId: CirEntityId): Boolean {
        val firstPackageSegment = classId.packageName.segments.firstOrNull()
        if (firstPackageSegment != "kotlinx" && firstPackageSegment != "kotlin") {
            return false
        }

        return classId in signedIntegers
                || classId in unsignedIntegers
                || classId in floatingPoints
                || classId in signedVarIntegers
                || classId in unsignedVarIntegers
                || classId in floatingPointVars
    }
}