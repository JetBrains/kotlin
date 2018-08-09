/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

import kotlin.*
import kotlin.internal.InlineOnly
import kotlin.internal.apiVersionIsAtLeast

/**
 * Returns an immutable list containing only the specified object [element].
 * The returned list is serializable.
 * @sample samples.collections.Collections.Lists.singletonReadOnlyList
 */
public fun <T> listOf(element: T): List<T> = java.util.Collections.singletonList(element)


/**
 * Returns a list containing the elements returned by this enumeration
 * in the order they are returned by the enumeration.
 * @sample samples.collections.Collections.Lists.listFromEnumeration
 */
@kotlin.internal.InlineOnly
public inline fun <T> java.util.Enumeration<T>.toList(): List<T> = java.util.Collections.list(this)


@kotlin.internal.InlineOnly
internal actual inline fun copyToArrayImpl(collection: Collection<*>): Array<Any?> =
    kotlin.jvm.internal.collectionToArray(collection)

@kotlin.internal.InlineOnly
internal actual inline fun <T> copyToArrayImpl(collection: Collection<*>, array: Array<T>): Array<T> =
    kotlin.jvm.internal.collectionToArray(collection, array as Array<Any?>) as Array<T>

// copies typed varargs array to array of objects
internal actual fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?> =
    if (isVarargs && this.javaClass == Array<Any?>::class.java)
    // if the array came from varargs and already is array of Any, copying isn't required
        @Suppress("UNCHECKED_CAST") (this as Array<Any?>)
    else
        java.util.Arrays.copyOf(this, this.size, Array<Any?>::class.java)


@PublishedApi
@SinceKotlin("1.3")
@InlineOnly
internal actual inline fun checkIndexOverflow(index: Int): Int {
    if (index < 0) {
        if (apiVersionIsAtLeast(1, 3, 0))
            throwIndexOverflow()
        else
            throw ArithmeticException("Index overflow has happened.")
    }
    return index
}

@PublishedApi
@SinceKotlin("1.3")
@InlineOnly
internal actual inline fun checkCountOverflow(count: Int): Int {
    if (count < 0) {
        if (apiVersionIsAtLeast(1, 3, 0))
            throwCountOverflow()
        else
            throw ArithmeticException("Count overflow has happened.")
    }
    return count
}

