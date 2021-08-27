/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.cir.CirEntityId

private typealias BitWidth = Int

private class SubstitutableNumbers(private val numbers: Map<CirEntityId, BitWidth>) {
    operator fun contains(id: CirEntityId) = id in numbers
    fun choose(first: CirClassType, second: CirClassType): CirClassType? {
        val firstBitWidth = numbers[first.classifierId] ?: return null
        val secondBitWidth = numbers[second.classifierId] ?: return null
        return if (secondBitWidth > firstBitWidth) second else first
    }
}

private val signedIntegers = SubstitutableNumbers(
    mapOf(
        CirEntityId.create("kotlin/Byte") to 8,
        CirEntityId.create("kotlin/Short") to 16,
        CirEntityId.create("kotlin/Int") to 32,
        CirEntityId.create("kotlin/Long") to 64
    )
)

private val unsignedIntegers = SubstitutableNumbers(
    mapOf(
        CirEntityId.create("kotlin/UByte") to 8,
        CirEntityId.create("kotlin/UShort") to 16,
        CirEntityId.create("kotlin/UInt") to 32,
        CirEntityId.create("kotlin/ULong") to 64
    )
)

private val floatingPoints = SubstitutableNumbers(
    mapOf(
        CirEntityId.create("kotlin/Float") to 32,
        CirEntityId.create("kotlin/Double") to 64,
    )
)

private val signedVarIntegers = SubstitutableNumbers(
    mapOf(
        CirEntityId.create("kotlinx/cinterop/ByteVarOf") to 8,
        CirEntityId.create("kotlinx/cinterop/ShortVarOf") to 16,
        CirEntityId.create("kotlinx/cinterop/IntVarOf") to 32,
        CirEntityId.create("kotlinx/cinterop/LongVarOf") to 64,
    )
)

private val unsignedVarIntegers = SubstitutableNumbers(
    mapOf(
        CirEntityId.create("kotlinx/cinterop/UByteVarOf") to 8,
        CirEntityId.create("kotlinx/cinterop/UShortVarOf") to 16,
        CirEntityId.create("kotlinx/cinterop/UIntVarOf") to 32,
        CirEntityId.create("kotlinx/cinterop/ULongVarOf") to 64,
    )
)

private val floatingPointVars = SubstitutableNumbers(
    mapOf(
        CirEntityId.create("kotlinx/cinterop/FloatVarOf") to 32,
        CirEntityId.create("kotlinx/cinterop/DoubleVarOf") to 64,
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