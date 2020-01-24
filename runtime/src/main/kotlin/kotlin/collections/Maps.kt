/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

// creates a singleton copy of map, if there is specialization available in target platform, otherwise returns itself
@Suppress("NOTHING_TO_INLINE")
internal inline actual fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V> = toSingletonMap()

// creates a singleton copy of map
internal actual fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V>
        = with(entries.iterator().next()) { mutableMapOf(key to value) }


/**
 * Native map and set implementations do not make use of capacities or load factors.
 */
@PublishedApi
internal actual fun mapCapacity(expectedSize: Int) = expectedSize

/**
 * Checks a collection builder function capacity argument.
 * Does nothing, capacity is validated in List/Set/Map constructor
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
@PublishedApi
internal actual fun checkBuilderCapacity(capacity: Int) {
    require(capacity >= 0) { "capacity must be non-negative." }
}