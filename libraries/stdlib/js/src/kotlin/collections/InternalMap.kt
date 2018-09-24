/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * The common interface of [InternalStringMap] and [InternalHashCodeMap].
 */
internal interface InternalMap<K, V> : MutableIterable<MutableMap.MutableEntry<K, V>> {
    val equality: EqualityComparator
    val size: Int
    operator fun contains(key: K): Boolean
    operator fun get(key: K): V?

    fun put(key: K, value: V): V?
    fun remove(key: K): V?
    fun clear(): Unit

    fun createJsMap(): dynamic {
        val result = js("Object.create(null)")
        // force to switch object representation to dictionary mode
        // Using js-function due to JS_IR limitations
        js("result[\"foo\"] = 1")
        js("delete result[\"foo\"]")
        return result
    }
}