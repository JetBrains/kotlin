/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress(
    "TYPE_PARAMETER_AS_REIFIED",
    "WRONG_MODIFIER_TARGET",
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "UNUSED_PARAMETER"
)

package kotlin

import kotlin.wasm.internal.*

/**
 * Represents an array (specifically, a Java array when targeting the JVM platform).
 * Array instances can be created using the [arrayOf], [arrayOfNulls] and [emptyArray]
 * standard library functions.
 * See [Kotlin language documentation](https://kotlinlang.org/docs/reference/basic-types.html#arrays)
 * for more information on arrays.
 */
public class Array<T> @PublishedApi internal constructor(size: Int) {
    internal val storage: WasmAnyArray = WasmAnyArray(size)

    @WasmPrimitiveConstructor
    internal constructor(storage: WasmAnyArray)

    /**
     * Creates a new array with the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    public inline constructor(size: Int, init: (Int) -> T)

    /**
     * Returns the array element at the specified [index]. This method can be called using the
     * index operator.
     * ```
     * value = arr[index]
     * ```
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    @Suppress("UNCHECKED_CAST")
    public operator fun get(index: Int): T {
        rangeCheck(index, storage.len())
        return storage.get(index) as T
    }

    /**
     * Sets the array element at the specified [index] to the specified [value]. This method can
     * be called using the index operator.
     * ```
     * arr[index] = value
     * ```
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public operator fun set(index: Int, value: T) {
        rangeCheck(index, storage.len())
        storage.set(index, value)
    }

    /**
     * Returns the number of elements in the array.
     */
    public val size: Int
        get() = storage.len()

    /**
     * Creates an iterator for iterating over the elements of the array.
     */
    public operator fun iterator(): Iterator<T> = arrayIterator(this)
}

internal fun <T> arrayIterator(array: Array<T>) = object : Iterator<T> {
    var index = 0
    override fun hasNext() = index != array.size
    override fun next() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}

internal inline fun <reified T> createAnyArray(size: Int, init: (Int) -> T): Array<T> {
    val result = WasmAnyArray(size)
    result.fill(size, init)
    return Array(result)
}
