/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.name.ClassId

val KOTLIN_INT_ID = ClassId.fromString("kotlin/Int")
val KOTLIN_LONG_ID = ClassId.fromString("kotlin/Long")

val KOTLIN_UINT_ID = ClassId.fromString("kotlin/UInt")
val KOTLIN_ULONG_ID = ClassId.fromString("kotlin/ULong")

val INT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/IntVarOf")
val LONG_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/LongVarOf")

val UINT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/UIntVarOf")
val ULONG_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/ULongVarOf")

val INT_ARRAY_ID = ClassId.fromString("kotlin/IntArray")
val LONG_ARRAY_ID = ClassId.fromString("kotlin/LongArray")

val UINT_ARRAY_ID = ClassId.fromString("kotlin/UIntArray")
val ULONG_ARRAY_ID = ClassId.fromString("kotlin/ULongArray")

val INT_RANGE_ID = ClassId.fromString("kotlin/ranges/IntRange")
val LONG_RANGE_ID = ClassId.fromString("kotlin/ranges/LongRange")

val UINT_RANGE_ID = ClassId.fromString("kotlin/ranges/UIntRange")
val ULONG_RANGE_ID = ClassId.fromString("kotlin/ranges/ULongRange")

val INT_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/IntProgression")
val LONG_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/LongProgression")

val UINT_PROGRESSION = ClassId.fromString("kotlin/ranges/UIntProgression")
val ULONG_PROGRESSION = ClassId.fromString("kotlin/ranges/ULongProgression")

val PLATFORM_INT_ID = ClassId.fromString("kotlin/PlatformInt")
val PLATFORM_UINT_ID = ClassId.fromString("kotlin/PlatformUInt")

val PLATFORM_INT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/PlatformIntVarOf")
val PLATFORM_UINT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/PlatformUIntVarOf")

val PLATFORM_INT_ARRAY_ID = ClassId.fromString("kotlin/PlatformIntArray")
val PLATFORM_UINT_ARRAY_ID = ClassId.fromString("kotlin/PlatformUIntArray")

val PLATFORM_INT_RANGE_ID = ClassId.fromString("kotlin/ranges/PlatformIntRange")
val PLATFORM_UINT_RANGE_ID = ClassId.fromString("kotlin/ranges/PlatformUIntRange")

val PLATFORM_INT_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/PlatformIntProgression")
val PLATFORM_UINT_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/PlatformUIntProgression")

val PLATFORM_INTEGERS: Set<ClassId> = setOf(
    PLATFORM_INT_ID, PLATFORM_UINT_ID
)

val PLATFORM_VARIABLES: Set<ClassId> = setOf(
    PLATFORM_INT_VAR_OF_ID, PLATFORM_UINT_VAR_OF_ID
)

val PLATFORM_ARRAYS: Set<ClassId> = setOf(
    PLATFORM_INT_ARRAY_ID, PLATFORM_UINT_ARRAY_ID
)

val PLATFORM_RANGES: Set<ClassId> = setOf(
    PLATFORM_INT_RANGE_ID, PLATFORM_UINT_RANGE_ID
)

val PLATFORM_PROGRESSIONS: Set<ClassId> = setOf(
    PLATFORM_INT_PROGRESSION_ID, PLATFORM_UINT_PROGRESSION_ID
)

val PLATFORM_TYPES: Set<ClassId> =
    PLATFORM_INTEGERS + PLATFORM_VARIABLES + PLATFORM_ARRAYS + PLATFORM_RANGES + PLATFORM_PROGRESSIONS
