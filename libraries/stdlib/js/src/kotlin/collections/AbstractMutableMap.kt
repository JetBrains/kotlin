/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Based on GWT AbstractMap
 * Copyright 2007 Google Inc.
 */

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableMap] interface.
 *
 * The implementor is required to implement [entries] property, which should return mutable set of map entries, and [put] function.
 *
 * @param K the type of map keys. The map is invariant in its key type.
 * @param V the type of map values. The map is invariant in its value type.
 */
public actual abstract class AbstractMutableMap<K, V> protected actual constructor() : AbstractMap<K, V>(), MutableMap<K, V> {

    internal open fun createKeysView(): MutableSet<K> = HashMapKeysDefault(this)
    internal open fun createValuesView(): MutableCollection<V> = HashMapValuesDefault(this)

    private var keysView: MutableSet<K>? = null
    private var valuesView: MutableCollection<V>? = null

    actual override val keys: MutableSet<K>
        get() = keysView ?: createKeysView().also { keysView = it }

    actual override val values: MutableCollection<V>
        get() = valuesView ?: createValuesView().also { valuesView = it }

    actual override fun clear() {
        entries.clear()
    }

    @IgnorableReturnValue
    actual abstract override fun put(key: K, value: V): V?

    actual override fun putAll(from: Map<out K, V>) {
        checkIsMutable()
        for ((key, value) in from) {
            put(key, value)
        }
    }

    @IgnorableReturnValue
    actual override fun remove(key: K): V? {
        checkIsMutable()
        val iter = entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val k = entry.key
            if (key == k) {
                val value = entry.value
                iter.remove()
                return value
            }
        }
        return null
    }


    /**
     * This method is called every time when a mutating method is called on this mutable map.
     * Mutable maps that are built (frozen) must throw `UnsupportedOperationException`.
     */
    internal open fun checkIsMutable() {}
}
