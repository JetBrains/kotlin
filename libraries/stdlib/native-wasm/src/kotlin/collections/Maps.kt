/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

@PublishedApi
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
internal actual inline fun <K, V> buildMapInternal(builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> {
    return HashMap<K, V>().apply(builderAction).build()
}

@PublishedApi
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
internal actual inline fun <K, V> buildMapInternal(capacity: Int, builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> {
    return HashMap<K, V>(capacity).apply(builderAction).build()
}


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
internal actual fun mapCapacity(expectedSize: Int): Int = expectedSize

/**
 * Returns a new read-only map, mapping only the specified key to the
 * specified value.
 *
 * @sample samples.collections.Maps.Instantiation.mapFromPairs
 */
@SinceKotlin("1.9")
public actual fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V> = hashMapOf(pair)