/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import withType

external fun <T> Array(size: Int): Array<T>

@PublishedApi
internal fun <T> fillArrayVal(array: Array<T>, initValue: T): Array<T> {
    for (i in 0..array.size - 1) {
        array[i] = initValue
    }
    return array
}

internal inline fun <T> arrayWithFun(size: Int, init: (Int) -> T) = fillArrayFun(Array<T>(size), init)

internal inline fun <T> fillArrayFun(array: dynamic, init: (Int) -> T): Array<T> {
    val result = array.unsafeCast<Array<T>>()
    var i = 0
    while (i != result.size) {
        result[i] = init(i)
        ++i
    }
    return result
}

internal fun booleanArray(size: Int): BooleanArray = withType("BooleanArray", fillArrayVal(Array<Boolean>(size), false)).unsafeCast<BooleanArray>()

internal fun booleanArrayOf(arr: Array<Boolean>): BooleanArray = withType("BooleanArray", arr.asDynamic().slice()).unsafeCast<BooleanArray>()

//internal fun charArray(size: Int): CharArray = withType("CharArray", fillArrayVal(Array<Int>(size), 0)).unsafeCast<CharArray>()
internal fun charArray(size: Int): CharArray = withType("CharArray", fillArrayVal(Array<Char>(size), Char(0))).unsafeCast<CharArray>()

internal fun charArrayOf(arr: Array<Char>): CharArray = withType("CharArray", arr.asDynamic().slice()).unsafeCast<CharArray>()

internal fun longArray(size: Int): LongArray = withType("LongArray", fillArrayVal(Array<Long>(size), 0L)).unsafeCast<LongArray>()

internal fun longArrayOf(arr: Array<Long>): LongArray = withType("LongArray", arr.asDynamic().slice()).unsafeCast<LongArray>()

internal fun <T> arrayIterator(array: Array<T>) = object : Iterator<T> {
    var index = 0
    override fun hasNext() = index != array.size
    override fun next() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal fun booleanArrayIterator(array: BooleanArray) = object : BooleanIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextBoolean() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal fun byteArrayIterator(array: ByteArray) = object : ByteIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextByte() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal fun shortArrayIterator(array: ShortArray) = object : ShortIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextShort() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal fun charArrayIterator(array: CharArray) = object : CharIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextChar() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal fun intArrayIterator(array: IntArray) = object : IntIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextInt() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal fun floatArrayIterator(array: FloatArray) = object : FloatIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextFloat() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal fun doubleArrayIterator(array: DoubleArray) = object : DoubleIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextDouble() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal fun longArrayIterator(array: LongArray) = object : LongIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextLong() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}
