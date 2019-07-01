/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SetsKt")

package kotlin.collections


/**
 * Returns an immutable set containing only the specified object [element].
 * The returned set is serializable.
 */
public fun <T> setOf(element: T): Set<T> = java.util.Collections.singleton(element)


/**
 * Returns a new [java.util.SortedSet] with the given elements.
 */
public fun <T> sortedSetOf(vararg elements: T): java.util.TreeSet<T> = elements.toCollection(java.util.TreeSet<T>())

/**
 * Returns a new [java.util.SortedSet] with the given [comparator] and elements.
 */
public fun <T> sortedSetOf(comparator: Comparator<in T>, vararg elements: T): java.util.TreeSet<T> = elements.toCollection(java.util.TreeSet<T>(comparator))

