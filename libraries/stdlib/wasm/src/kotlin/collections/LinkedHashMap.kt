/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

actual open class LinkedHashMap<K, V> : MutableMap<K, V> {
    actual constructor() { TODO("Wasm stdlib: LinkedHashMap") }
    actual constructor(initialCapacity: Int) { TODO("Wasm stdlib: LinkedHashMap") }
    actual constructor(initialCapacity: Int, loadFactor: Float) { TODO("Wasm stdlib: LinkedHashMap") }
    actual constructor(original: Map<out K, V>) { TODO("Wasm stdlib: LinkedHashMap") }

    // From Map

    actual override val size: Int = TODO("Wasm stdlib: LinkedHashMap")
    actual override fun isEmpty(): Boolean = TODO("Wasm stdlib: LinkedHashMap")
    actual override fun containsKey(key: K): Boolean = TODO("Wasm stdlib: LinkedHashMap")
    actual override fun containsValue(value: V): Boolean = TODO("Wasm stdlib: LinkedHashMap")
    actual override fun get(key: K): V? = TODO("Wasm stdlib: LinkedHashMap")

    // From MutableMap

    actual override fun put(key: K, value: V): V? = TODO("Wasm stdlib: LinkedHashMap")
    actual override fun remove(key: K): V? = TODO("Wasm stdlib: LinkedHashMap")
    actual override fun putAll(from: Map<out K, V>) { TODO("Wasm stdlib: LinkedHashMap") }
    actual override fun clear() { TODO("Wasm stdlib: LinkedHashMap") }
    actual override val keys: MutableSet<K> = TODO("Wasm stdlib: LinkedHashMap")
    actual override val values: MutableCollection<V> = TODO("Wasm stdlib: LinkedHashMap")
    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = TODO("Wasm stdlib: LinkedHashMap")
}