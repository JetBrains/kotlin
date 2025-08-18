/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

/**
 * Specialized hash set implementation. Cannot store `-1L`. Open addressing.
 *
 * Size is always a power of two.
 */
class LongHashSet(sizeLog2: Int = 5, private val loadFactor: Float = 0.5f) : Iterable<Long>, Set<Long> {

    private var data: LongArray = LongArray(1 shl sizeLog2) { -1L }
    private var count: Int = 0
    private var growAt = growAtValue()
    private var sizeMask = data.size - 1

    override val size: Int
        get() = count

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun contains(element: Long): Boolean {
        val i = tryFind(element)

        return data[i] != -1L
    }

    override fun containsAll(elements: Collection<Long>): Boolean {
        return elements.all { contains(it) }
    }

    override fun iterator(): Iterator<Long> {
        return Itr()
    }

    fun add(element: Long): Boolean {
        if (element == -1L) error("Cannot store -1L")

        val i = tryFind(element)

        if (data[i] == -1L) {
            data[i] = element
            ++count
            if (count >= growAt) grow()
            return true
        }

        return false
    }

    private fun tryFind(element: Long): Int {
        var i = hash(element)
        while (data[i] != -1L && data[i] != element) {
            i = (i + 1) and sizeMask
        }
        return i
    }

    private fun grow() {
        val oldData = data
        data = LongArray(oldData.size * 2) { -1L }
        growAt = growAtValue()
        sizeMask = data.size - 1
        for (v in oldData) {
            if (v == -1L) continue
            data[tryFind(v)] = v
        }
    }

    private fun growAtValue() = (data.size * loadFactor).toInt()

    private fun hash(v: Long): Int {
        // This is 64-bit extension of a hashing method from Knuth's "The Art of Computer Programming".
        // The magic constant is the closest prime to 2^64 * phi, where phi is the golden ratio.
        val x = v * -7046029254386353223L
        return (x.toInt() xor (x ushr 32).toInt()) and sizeMask
    }

    private inner class Itr : Iterator<Long> {
        private var index = 0

        private fun findNext(): Int {
            while (index < data.size) {
                if (data[index] != -1L) return index
                ++index
            }
            return data.size
        }

        override fun hasNext() = findNext() != data.size

        override fun next() = data[findNext().also { ++index }]
    }
}

/**
 * Specialized hash map implementation. Cannot store `-1L`. Open addressing.
 *
 * Size is always a power of two.
 */
class LongHashMap<T>(sizeLog2: Int = 5, private val loadFactor: Float = 0.5f) : Iterable<Long>, Set<Long> {

    private var data: LongArray = LongArray(1 shl sizeLog2) { -1L }
    private var payload: Array<T?> = arrayOfNulls(1 shl sizeLog2)
    private var count: Int = 0
    private var growAt = growAtValue()
    private var sizeMask = data.size - 1

    override val size: Int
        get() = count

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun contains(element: Long): Boolean {
        val i = tryFind(element)

        return data[i] != -1L
    }

    override fun containsAll(elements: Collection<Long>): Boolean {
        return elements.all { contains(it) }
    }

    override fun iterator(): Iterator<Long> {
        return Itr()
    }

    operator fun set(key: Long, value: T): Boolean {
        if (key == -1L) error("Cannot store -1L")

        val i = tryFind(key)

        payload[i] = value
        if (data[i] == -1L) {
            data[i] = key
            ++count
            if (count >= growAt) grow()
            return true
        }

        return false
    }

    operator fun get(key: Long): T? {
        if (key == -1L) error("Cannot store -1L")

        return payload[tryFind(key)]
    }

    private fun tryFind(element: Long): Int {
        var i = hash(element)
        while (data[i] != -1L && data[i] != element) {
            i = (i + 1) and sizeMask
        }
        return i
    }

    private fun grow() {
        val oldData = data
        val oldPayload = payload
        data = LongArray(oldData.size * 2) { -1L }
        payload = arrayOfNulls(oldPayload.size * 2)
        growAt = growAtValue()
        sizeMask = data.size - 1
        for (i in oldData.indices) {
            val v = oldData[i]
            if (v == -1L) continue
            val j = tryFind(v)
            data[j] = v
            payload[j] = oldPayload[i]
        }
    }

    private fun growAtValue() = (data.size * loadFactor).toInt()

    private fun hash(v: Long): Int {
        // This is 64-bit extension of a hashing method from Knuth's "The Art of Computer Programming".
        // The magic constant is the closest prime to 2^64 * phi, where phi is the golden ratio.
        val x = v * -7046029254386353223L
        return (x.toInt() xor (x ushr 32).toInt()) and sizeMask
    }

    private inner class Itr : Iterator<Long> {
        private var index = 0

        private fun findNext(): Int {
            while (index < data.size) {
                if (data[index] != -1L) return index
                ++index
            }
            return data.size
        }

        override fun hasNext() = findNext() != data.size

        override fun next() = data[findNext().also { ++index }]
    }
}