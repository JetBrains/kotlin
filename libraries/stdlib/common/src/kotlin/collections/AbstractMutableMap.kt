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
 * @param K the type of map keys. The map is invariant in its key type.
 * @param V the type of map values. The map is invariant in its value type.
 */
@SinceKotlin("1.3")
public expect abstract class AbstractMutableMap<K, V> : MutableMap<K, V> {
    protected constructor()

    /**
     * Associates the specified [value] with the specified [key] in the map.
     *
     * This method is redeclared as abstract, because it's not implemented in the base class,
     * so it must be always overridden in the concrete mutable collection implementation.
     *
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    @IgnorableReturnValue
    abstract override fun put(key: K, value: V): V?

    abstract override val entries: MutableSet<MutableMap.MutableEntry<K, V>>

    override val keys: MutableSet<K>
    override val size: Int
    override val values: MutableCollection<V>
    override fun clear()
    override fun containsKey(key: K): Boolean
    override fun containsValue(value: V): Boolean
    override fun get(key: K): V?
    override fun isEmpty(): Boolean
    override fun putAll(from: Map<out K, V>)
    @IgnorableReturnValue
    override fun remove(key: K): V?
}
