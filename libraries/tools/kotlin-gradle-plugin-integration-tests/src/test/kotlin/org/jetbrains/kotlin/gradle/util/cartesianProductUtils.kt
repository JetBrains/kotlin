/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

/**
 * Cartesian product of two and more collections.
 * Returns a sequence of all possible combinations between given elements.
 * Combination is represented as a list with indexes matching order of inputs
 * i.e.
 * ```
 * cartesianProductOf(listOf("a", "b"), listOf(1, 2), listOf(4.3, 6.3)).map { list ->
 *    list[0] // will contain elements of the first argument e.g. "a"
 *    list[1] // will contain elements of the second argument e.g. 2
 *    list[2] // will contain elements of the second argument e.g. 4.3
 *    // and so on
 * }
 * ```
 */
fun cartesianProductOf(
    first: Iterable<Any?>,
    second: Iterable<Any?>,
    vararg rest: Iterable<Any?>,
): Sequence<List<Any?>> {
    var result: Sequence<Pair<Any?, Any?>> = first x second
    for (restItem in rest) { result = result x restItem }
    return result.flattenPairs
}

/**
 * Cartesian product of two collections.
 * Returns a sequence of all possible pairs between elements from [this] and [that]
 */
infix fun <A, B> Iterable<A>.x(that: Iterable<B>): Sequence<Pair<A, B>> = sequence {
    for (a in this@x) {
        for (b in that) {
            yield(a to b)
        }
    }
}

/**
 * Cartesian product of a sequence and a collection.
 * Returns a sequence of all possible pairs between elements from [this] and [that]
 */
infix fun <A, B> Sequence<A>.x(that: Iterable<B>): Sequence<Pair<A, B>> = sequence {
    for (a in this@x) {
        for (b in that) {
            yield(a to b)
        }
    }
}

private val Pair<Any?, Any?>.flattenPair: List<Any?>
    get() {
        val list = mutableListOf<Any?>()
        val pairs = ArrayDeque<Any?>()
        pairs.add(this)

        while (pairs.isNotEmpty()) {
            val e = pairs.removeLast()
            if (e is Pair<*, *>) {
                pairs.addLast(e.second)
                pairs.addLast(e.first)
            } else {
                list.add(e)
            }
        }

        return list
    }

private val Sequence<Pair<Any?, Any?>>.flattenPairs: Sequence<List<Any?>> get() = map { it.flattenPair }
