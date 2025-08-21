/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

/**
 * Provides some bulk operations needed for devirtualization
 */
internal class CustomBitSet private constructor(size: Int, data: LongArray) {
    private var size = size
    private var data = data

    constructor() : this(0, EMPTY)

    constructor(nodesCount: Int) : this(0, LongArray((nodesCount shr 6) + 1))

    private fun ensureCapacity(index: Int) {
        if (data.size <= index) {
            val oldData = data
            data = LongArray((oldData.size * 2).coerceAtLeast(index + 1))
            oldData.copyInto(data)
        }
        if (size <= index) size = index + 1
    }

    fun set(bitIndex: Int) {
        val index = bitIndex shr 6
        val offset = bitIndex and 0x3f
        ensureCapacity(index)
        data[index] = data[index] or (1L shl offset)
    }

    fun clear(bitIndex: Int) {
        val index = bitIndex shr 6
        val offset = bitIndex and 0x3f
        ensureCapacity(index)
        data[index] = data[index] and (1L shl offset).inv()
    }

    operator fun get(bitIndex: Int): Boolean {
        val index = bitIndex shr 6
        val offset = bitIndex and 0x3f
        return index < size && (data[index] and (1L shl offset)) != 0L
    }

    operator fun set(bitIndex: Int, value: Boolean) {
        if (value) {
            set(bitIndex)
        } else {
            clear(bitIndex)
        }
    }

    fun cardinality(): Int {
        var cardinality = 0
        for (i in 0 until size) {
            cardinality += data[i].countOneBits()
        }
        return cardinality
    }

    inline fun forEachBit(block: (Int) -> Unit) {
        for (index in 0 until size) {
            var d = data[index]
            val idx = index shl 6
            while (d != 0L) {
                val t = d and -d
                d -= t
                block(idx + t.countTrailingZeroBits())
            }
        }
    }

    fun clear() {
        data.fill(0L)
        size = 0
    }

    fun or(another: CustomBitSet) {
        val adata = another.data
        val asize = another.size
        ensureCapacity(asize - 1)
        for (i in 0 until asize) {
            data[i] = data[i] or adata[i]
        }
    }

    fun orWithFilterHasChanged(another: CustomBitSet): Boolean {
        val adata = another.data
        val asize = another.size
        ensureCapacity(asize - 1)
        var acc = 0L
        for (i in 0 until asize) {
            val d = data[i]
            val dd = d or adata[i]
            acc = acc or (dd xor d)
            data[i] = dd
        }
        return acc != 0L
    }


    fun orWithFilterHasChanged(another: CustomBitSet, filter: CustomBitSet): Boolean {
        val fdata = filter.data
        val fsize = filter.size
        val adata = another.data
        val asize = another.size
        ensureCapacity(asize - 1)

        var acc = 0L
        for (i in 0 until asize.coerceAtMost(fsize)) {
            val d = data[i]
            val fd = fdata[i]
            val dd = d or (adata[i] and fd)
            acc = acc or (dd xor d)
            data[i] = dd
        }

        return acc != 0L
    }

    fun and(another: CustomBitSet) {
        val adata = another.data
        val asize = another.size
        ensureCapacity(asize - 1)
        for (i in 0 until asize) {
            data[i] = data[i] and adata[i]
        }
        for (i in asize until size) {
            data[i] = 0L
        }
    }

    fun andNot(another: CustomBitSet) {
        val adata = another.data
        val asize = another.size
        ensureCapacity(asize - 1)
        for (i in 0 until asize) {
            data[i] = data[i] and adata[i].inv()
        }
    }

    fun copy(): CustomBitSet {
        return CustomBitSet(size, data.copyOf())
    }

    val isEmpty
        get(): Boolean {
            for (i in 0 until size) {
                if (data[i] != 0L) return false
            }
            return true
        }

    companion object {
        private val EMPTY = LongArray(0)
    }
}