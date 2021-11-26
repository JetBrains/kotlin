/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal

class ArrayAsList<T>(val array: Array<T>) : AbstractList<T>(), RandomAccess {
    override val size: Int get() = array.size
    override fun isEmpty(): Boolean = array.isEmpty()
    override fun contains(element: T): Boolean = array.contains(element)
    override fun get(index: Int): T = array[index]
    override fun indexOf(element: T): Int = array.indexOf(element)
    override fun lastIndexOf(element: T): Int = array.lastIndexOf(element)
}