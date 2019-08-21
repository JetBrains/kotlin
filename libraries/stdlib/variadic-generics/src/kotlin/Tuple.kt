/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

class Tuple<Ts>(size: Int = 0) : Iterable<Any?> {
    private val elements: Array<Any?> = arrayOfNulls(size)
    val size: Int
        get() = elements.size

    operator fun get(index: Int): Any? = elements[index]
    operator fun set(index: Int, value: Any?) {
        elements[index] = value
    }

    fun clone(): Tuple<Ts> {
        val newTuple = Tuple<Ts>(size)
        for (i in 0..elements.lastIndex) {
            newTuple[i] = get(i)
        }
        return newTuple
    }

    override fun iterator(): Iterator<Any?> {
        return object : Iterator<Any?> {
            private var currentIndex = 0
            override fun next() = get(currentIndex++)
            override fun hasNext() = currentIndex < size
        }
    }
}

operator fun <Ts> Tuple<Ts>.component1(): Any? = get(0)
operator fun <Ts> Tuple<Ts>.component2(): Any? = get(1)
operator fun <Ts> Tuple<Ts>.component3(): Any? = get(2)
operator fun <Ts> Tuple<Ts>.component4(): Any? = get(3)
operator fun <Ts> Tuple<Ts>.component5(): Any? = get(4)
