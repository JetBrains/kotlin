/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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

    @Suppress("TYPE_PARAMETER_AS_REIFIED", "UNUSED_PARAMETER", "CAST_NEVER_SUCCEEDS")
    @WasmPrimitiveConstructor
    internal constructor(storage: WasmAnyArray) : this(check(false) as Int)

    /**
     * Creates a new array with the specified [size], where each element is calculated by calling the specified
     * [init] function.
     *
     * The function [init] is called for each array element sequentially starting from the first one.
     * It should return the value for an array element given its index.
     */
    // TODO: it have to be inline 
    @Suppress("TYPE_PARAMETER_AS_REIFIED")
    public constructor(size: Int, init: (Int) -> T) : this(size) {
        storage.fill(size, init)
    }

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

