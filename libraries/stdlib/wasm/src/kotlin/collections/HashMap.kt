/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

actual open class HashMap<K, V> : MutableMap<K, V> {
    actual constructor() { TODO("Wasm stdlib: HashMap") }
    actual constructor(initialCapacity: Int) { TODO("Wasm stdlib: HashMap") }
    actual constructor(initialCapacity: Int, loadFactor: Float) { TODO("Wasm stdlib: HashMap") }
    actual constructor(original: Map<out K, V>) { TODO("Wasm stdlib: HashMap") }

    // From Map

    actual override val size: Int = TODO("Wasm stdlib: HashMap")
    actual override fun isEmpty(): Boolean = TODO("Wasm stdlib: HashMap")
    actual override fun containsKey(key: K): Boolean = TODO("Wasm stdlib: HashMap")
    actual override fun containsValue(value: @UnsafeVariance V): Boolean = TODO("Wasm stdlib: HashMap")
    actual override operator fun get(key: K): V? = TODO("Wasm stdlib: HashMap")

    // From MutableMap

    actual override fun put(key: K, value: V): V? = TODO("Wasm stdlib: HashMap")
    actual override fun remove(key: K): V? = TODO("Wasm stdlib: HashMap")
    actual override fun putAll(from: Map<out K, V>) { TODO("Wasm stdlib: HashMap") }
    actual override fun clear() { TODO("Wasm stdlib: HashMap") }
    actual override val keys: MutableSet<K> = TODO("Wasm stdlib: HashMap")
    actual override val values: MutableCollection<V> = TODO("Wasm stdlib: HashMap")
    actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = TODO("Wasm stdlib: HashMap")
}