/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableMap] interface.
 *
 * The implementor is required to implement [entries] property, which should return mutable set of map entries, and [put] function.
 *
 * @param K the type of map keys. The map is invariant on its key type.
 * @param V the type of map values. The map is invariant on its value type.
 */
@SinceKotlin("1.3")
public actual abstract class AbstractMutableMap<K, V> : MutableMap<K, V> {
    actual protected constructor()

    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    abstract actual override fun put(key: K, value: V): V?

    abstract actual override val entries: MutableSet<MutableMap.MutableEntry<K, V>>

    actual override val keys: MutableSet<K> = TODO("Wasm stdlib: AbstractMutableMap")
    actual override val size: Int = TODO("Wasm stdlib: AbstractMutableMap")
    actual override val values: MutableCollection<V> = TODO("Wasm stdlib: AbstractMutableMap")
    actual override fun clear() { TODO("Wasm stdlib: AbstractMutableMap") }
    actual override fun containsKey(key: K): Boolean = TODO("Wasm stdlib: AbstractMutableMap")
    actual override fun containsValue(value: V): Boolean = TODO("Wasm stdlib: AbstractMutableMap")
    actual override fun get(key: K): V? = TODO("Wasm stdlib: AbstractMutableMap")
    actual override fun isEmpty(): Boolean = TODO("Wasm stdlib: AbstractMutableMap")
    actual override fun putAll(from: Map<out K, V>) { TODO("Wasm stdlib: AbstractMutableMap") }
    actual override fun remove(key: K): V? = TODO("Wasm stdlib: AbstractMutableMap")
}