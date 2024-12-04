/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

fun <T : Any> T?.toSetOrEmpty(): Set<T> =
    if (this == null) emptySet() else setOf(this)

/**
 * Merges two Map of Sets to new map
 * Example:
 * { 1: ['a', 'b'], 2: ['c'] }
 * merge
 * { 0: ['x'], 1: ['c'], 42: ['y'] }
 * =
 * { 0: ['x'], 1: ['a', 'b', 'c'], 2: ['c'], 42: ['y'] }
 */
infix fun <K, V> Map<K, Set<V>>.mergeWith(that: Map<K, Set<V>>): Map<K, Set<V>> {
    val result = mutableMapOf<K, Set<V>>()
    for ((k, setOfV) in this) {
        result[k] = setOfV + that[k].orEmpty()
    }

    val uniqueEntriesFromRight = that - this.keys
    result.putAll(uniqueEntriesFromRight)

    return result
}