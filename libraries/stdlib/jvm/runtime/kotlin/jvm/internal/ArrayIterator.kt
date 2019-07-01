/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

private class ArrayIterator<T>(val array: Array<T>) : Iterator<T> {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun next() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

public fun <T> iterator(array: Array<T>): Iterator<T> = ArrayIterator(array)
