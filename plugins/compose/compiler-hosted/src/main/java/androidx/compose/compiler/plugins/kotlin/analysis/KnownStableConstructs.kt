/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.analysis

import kotlin.coroutines.EmptyCoroutineContext

/**
 * This is a registry of APIs that are defined outside of Compose that we know to be stable but
 * cannot annotate with `Stable` or `Immutable`.
 *
 * For all of the functions and types listed in these collections, we associate them with a bitmask.
 * This mask corresponds to the bitmask returned by
 * [androidx.compose.compiler.plugins.kotlin.analysis.stabilityParamBitmask] for general classes.
 * It defines how the generic parameters of a class/function influence its stability.
 *
 * A bit set to `1` in this mask indicates that the construct cannot be considered stable unless the
 * generic type at that position is also a stable type. If a bit is set to `0`, it indicates that
 * its corresponding generic type has no influence on whether the construct is stable.
 *
 * The bit at index 0 in this mask corresponds to the first generic type, and each subsequently
 * higher bit moves one generic type further to the right as they're defined. If the construct
 * doesn't have any generic types, it will have a mask of `0`.
 */
object KnownStableConstructs {

    val stableTypes = mapOf(
        Pair::class.qualifiedName!! to 0b11,
        Triple::class.qualifiedName!! to 0b111,
        Comparator::class.qualifiedName!! to 0,
        Result::class.qualifiedName!! to 0b1,
        ClosedRange::class.qualifiedName!! to 0b1,
        ClosedFloatingPointRange::class.qualifiedName!! to 0b1,
        // Guava
        "com.google.common.collect.ImmutableList" to 0b1,
        "com.google.common.collect.ImmutableEnumMap" to 0b11,
        "com.google.common.collect.ImmutableMap" to 0b11,
        "com.google.common.collect.ImmutableEnumSet" to 0b1,
        "com.google.common.collect.ImmutableSet" to 0b1,
        // Kotlinx immutable
        "kotlinx.collections.immutable.ImmutableCollection" to 0b1,
        "kotlinx.collections.immutable.ImmutableList" to 0b1,
        "kotlinx.collections.immutable.ImmutableSet" to 0b1,
        "kotlinx.collections.immutable.ImmutableMap" to 0b11,
        "kotlinx.collections.immutable.PersistentCollection" to 0b1,
        "kotlinx.collections.immutable.PersistentList" to 0b1,
        "kotlinx.collections.immutable.PersistentSet" to 0b1,
        "kotlinx.collections.immutable.PersistentMap" to 0b11,
        // Dagger
        "dagger.Lazy" to 0b1,
        // Coroutines
        EmptyCoroutineContext::class.qualifiedName!! to 0,
    )

    // TODO: buildList, buildMap, buildSet, etc.
    val stableFunctions = mapOf(
        "kotlin.collections.emptyList" to 0,
        "kotlin.collections.listOf" to 0b1,
        "kotlin.collections.listOfNotNull" to 0b1,
        "kotlin.collections.mapOf" to 0b11,
        "kotlin.collections.emptyMap" to 0,
        "kotlin.collections.setOf" to 0b1,
        "kotlin.collections.emptySet" to 0,
        "kotlin.to" to 0b11,
        // Kotlinx immutable
        "kotlinx.collections.immutable.immutableListOf" to 0b1,
        "kotlinx.collections.immutable.immutableSetOf" to 0b1,
        "kotlinx.collections.immutable.immutableMapOf" to 0b11,
        "kotlinx.collections.immutable.persistentListOf" to 0b1,
        "kotlinx.collections.immutable.persistentSetOf" to 0b1,
        "kotlinx.collections.immutable.persistentMapOf" to 0b11,
    )
}
