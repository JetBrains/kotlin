/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")


package kotlin.collections

import kotlin.contracts.*


/**
 * Returns a single list of all elements from all arrays in the given array.
 * @sample samples.collections.Arrays.Transformations.flattenArray
 */
public fun <T> Array<out Array<out T>>.flatten(): List<T> {
    val result = ArrayList<T>(sumBy { it.size })
    for (element in this) {
        result.addAll(element)
    }
    return result
}

/**
 * Returns a pair of lists, where
 * *first* list is built from the first values of each pair from this array,
 * *second* list is built from the second values of each pair from this array.
 * @sample samples.collections.Arrays.Transformations.unzipArray
 */
public fun <T, R> Array<out Pair<T, R>>.unzip(): Pair<List<T>, List<R>> {
    val listT = ArrayList<T>(size)
    val listR = ArrayList<R>(size)
    for (pair in this) {
        listT.add(pair.first)
        listR.add(pair.second)
    }
    return listT to listR
}

/**
 * Returns `true` if this nullable array is either null or empty.
 * @sample samples.collections.Arrays.Usage.arrayIsNullOrEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
public inline fun Array<*>?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }

    return this == null || this.isEmpty()
}

/**
 * Returns this array if it's not empty
 * or the result of calling [defaultValue] function if the array is empty.
 *
 * @sample samples.collections.Arrays.Usage.arrayIfEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
@Suppress("UPPER_BOUND_CANNOT_BE_ARRAY")
public inline fun <C, R> C.ifEmpty(defaultValue: () -> R): R where C : Array<*>, C : R =
    if (isEmpty()) defaultValue() else this
