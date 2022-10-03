/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

/**
 * Own implementation of SetMultiMap. The reason to provide own implementation, and not to re-use from IntelliJ core or compiler.fir.cones:
 * to break classpath compile/run inconsistency for `core-impl` module.
 */
class SetMultimap<K, V> {
    private val map: MutableMap<K, MutableCollection<V>> = hashMapOf()

    operator fun get(key: K): MutableCollection<V>? = map[key]

    fun put(key: K, value: V) {
        if (map[key] == null) map[key] = mutableSetOf()
        map[key]!!.add(value)
    }

    fun putAll(key: K, value: Iterable<V>) {
        if (map[key] == null) map[key] = mutableSetOf()
        map[key]!!.addAll(value)
    }

    fun putIfAbsent(key: K, value: V) {
        if (map[key] == null) map[key] = mutableSetOf()
        if (!map[key]!!.contains(value)) map[key]!!.add(value)
    }

    fun remove(key: K, value: V): Boolean {
        return if (map[key] != null) map[key]!!.remove(value) else false
    }

    fun containsKey(key: K?): Boolean = map.containsKey(key)

    fun remove(key: K) {
        map.remove(key)
    }

    fun values(): MutableCollection<MutableCollection<V>> {
        return map.values
    }

    fun entries(): MutableSet<MutableMap.MutableEntry<K, MutableCollection<V>>> {
        return map.entries
    }

    fun size(): Int {
        var size = 0
        for (value in map.values) size += value.size
        return size
    }
}