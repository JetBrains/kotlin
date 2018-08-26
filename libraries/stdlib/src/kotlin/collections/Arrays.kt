/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ArraysKt")


package kotlin.collections


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

private val hexArray = CharArray(16) { if (it < 10) '0' + it else 'a' + (it - 10) }

/**
* Creates a string from bytes represented as hexadecimal numbers.
*/
fun ByteArray.toHexString(): String {
    val builder = StringBuilder(size * 2)
    for (byte in this) {
        val v = byte.toInt() and 0xFF
        builder.append(hexArray[v.ushr(4)])
        builder.append(hexArray[v and 0x0F])
    }
    return builder.toString()
}