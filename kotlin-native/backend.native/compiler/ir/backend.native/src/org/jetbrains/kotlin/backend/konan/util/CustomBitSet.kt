/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import it.unimi.dsi.fastutil.ints.IntArraySet
import it.unimi.dsi.fastutil.ints.IntSet

const val LAZY_CONVERSION_THRESHOLD = 8

/**
 * Provides some bulk operations needed for devirtualization.
 *
 * Mutating operations ([or], [orHasChanged], [orWithFilterHasChanged], [and], [andNot])
 * must not be called with `this` as any of their bitset arguments. Self-application
 * would either iterate over a set that is being modified, or read a data array that
 * may be reallocated mid-operation. Callers must [copy] before applying such an
 * operation to itself.
 */
internal class CustomBitSet private constructor(size: Int, data: LongArray) {
    var size = size
        private set
    private var data = data
    private var lazy: IntSet? = null

    internal val isLazy: Boolean get() = lazy != null

    /** For testing: the length of the underlying word array (independent of [size]). */
    internal val dataCapacity: Int get() = data.size

    constructor() : this(0, EMPTY) {
        lazy = IntArraySet(LAZY_CONVERSION_THRESHOLD)
    }

    constructor(nodesCount: Int) : this(0, LongArray((nodesCount shr 6) + 1))

    private fun buildFromLazy() {
        val lazy = lazy ?: return
        this.lazy = null
        if (lazy.isNotEmpty()) ensureCapacity(lazy.max() ushr 6)
        lazy.forEach(::set)
    }

    private fun ensureCapacity(index: Int) {
        check(!isLazy) { "ensureCapacity called while in lazy mode" }
        if (data.size <= index) {
            val oldData = data
            data = LongArray((oldData.size * 2).coerceAtLeast(index + 1))
            oldData.copyInto(data)
        }
        if (size <= index) size = index + 1
    }

    private fun updateLazySize() {
        check(isLazy) { "updateLazySize called while in dense mode" }
        size = lazy!!.maxOrNull()?.let { it.ushr(6) + 1 } ?: 0
    }

    private fun shrinkDenseSize() {
        check(!isLazy) { "shrinkDenseSize called while in lazy mode" }
        while (size > 0 && data[size - 1] == 0L) size--
    }

    fun set(bitIndex: Int) {
        lazy?.let { lazy ->
            size = size.coerceAtLeast(bitIndex.ushr(6) + 1)
            lazy.add(bitIndex)
            if (lazy.size == LAZY_CONVERSION_THRESHOLD) buildFromLazy()
            return
        }
        val index = bitIndex shr 6
        val offset = bitIndex and 0x3f
        ensureCapacity(index)
        data[index] = data[index] or (1L shl offset)
    }

    fun clear(bitIndex: Int) {
        lazy?.let { lazy ->
            lazy.remove(bitIndex)
            updateLazySize()
            return
        }
        val index = bitIndex shr 6
        if (index >= size) {
            check(index >= data.size || data[index] == 0L)
            return
        }
        val offset = bitIndex and 0x3f
        data[index] = data[index] and (1L shl offset).inv()
        shrinkDenseSize()
    }

    operator fun get(bitIndex: Int): Boolean {
        lazy?.let { lazy ->
            return bitIndex in lazy
        }
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
        lazy?.let { lazy ->
            return lazy.size
        }
        var cardinality = 0
        for (i in 0 until size) {
            cardinality += data[i].countOneBits()
        }
        return cardinality
    }

    inline fun forEachBit(block: (Int) -> Unit) {
        lazy?.let { lazy ->
            lazy.forEach(block)
            return
        }
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

    inline fun forEachWord(block: (Long) -> Unit) {
        buildFromLazy()
        for (i in 0..<size)
            block(data[i])
    }

    fun clear() {
        size = 0
        lazy?.let { lazy ->
            lazy.clear()
            return
        }
        data.fill(0L)
    }

    fun or(another: CustomBitSet) {
        require(another !== this) { "or() must not be called with this as the argument" }
        another.lazy?.let { alazy ->
            alazy.forEach { set(it) }
            return
        }
        buildFromLazy()
        val adata = another.data
        val asize = another.size
        ensureCapacity(asize - 1)
        for (i in 0 until asize) {
            data[i] = data[i] or adata[i]
        }
    }

    fun orHasChanged(another: CustomBitSet): Boolean {
        require(another !== this) { "orHasChanged() must not be called with this as the argument" }
        another.lazy?.let { alazy ->
            var changed = false
            alazy.forEach {
                changed = changed or !get(it)
                set(it)
            }
            return changed
        }
        buildFromLazy()
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
        require(another !== this) { "orWithFilterHasChanged() must not be called with this as `another`" }
        require(filter !== this) { "orWithFilterHasChanged() must not be called with this as `filter`" }
        another.lazy?.let { alazy ->
            var changed = false
            alazy.forEach {
                if (filter[it]) {
                    changed = changed or !get(it)
                    set(it)
                }
            }
            return changed
        }
        buildFromLazy()
        filter.lazy?.let { flazy ->
            var changed = false
            flazy.forEach { bit ->
                if (!this[bit] && another[bit]) {
                    changed = true
                    set(bit)
                }
            }
            return changed
        }
        val fdata = filter.data
        val fsize = filter.size
        val adata = another.data
        val asize = another.size
        val minSize = minOf(asize, fsize)
        ensureCapacity(minSize - 1)

        var acc = 0L
        for (i in 0 until minSize) {
            val d = data[i]
            val fd = fdata[i]
            val dd = d or (adata[i] and fd)
            acc = acc or (dd xor d)
            data[i] = dd
        }
        shrinkDenseSize()

        return acc != 0L
    }

    fun and(another: CustomBitSet) {
        require(another !== this) { "and() must not be called with this as the argument" }
        lazy?.let { lazy ->
            lazy.retainAll { another[it] }
            updateLazySize()
            return
        }
        another.lazy?.let { alazy ->
            val newLazy = IntArraySet(LAZY_CONVERSION_THRESHOLD)
            alazy.forEach { bit ->
                if (this[bit]) {
                    newLazy.add(bit)
                }
            }
            lazy = newLazy
            data = EMPTY
            updateLazySize()
            return
        }
        // and only clears bits, so this.size never needs to grow
        val adata = another.data
        val minSize = minOf(size, another.size)
        for (i in 0 until minSize) {
            data[i] = data[i] and adata[i]
        }
        for (i in minSize until size) {
            data[i] = 0L
        }
        shrinkDenseSize()
    }

    fun andNot(another: CustomBitSet) {
        require(another !== this) { "andNot() must not be called with this as the argument" }
        lazy?.let { lazy ->
            lazy.retainAll { !another[it] }
            updateLazySize()
            return
        }
        another.lazy?.let { alazy ->
            alazy.forEach { clear(it) }
            return
        }
        // andNot only clears bits, so this.size never needs to grow
        val adata = another.data
        val minSize = minOf(size, another.size)
        for (i in 0 until minSize) {
            data[i] = data[i] and adata[i].inv()
        }
        // Every word over minSize stays unchanged
        shrinkDenseSize()
    }

    fun intersects(another: CustomBitSet): Boolean {
        lazy?.let { lazy ->
            return lazy.any { another[it] }
        }
        another.lazy?.let { alazy ->
            return alazy.any { this[it] }
        }
        val adata = another.data
        val minSize = kotlin.math.min(size, another.size)
        for (i in 0..<minSize) {
            if (data[i] and adata[i] != 0L) return true
        }

        return false
    }

    operator fun contains(another: CustomBitSet): Boolean {
        lazy?.let { lazy ->
            another.forEachBit {
                if (it !in lazy) return false
            }
            return true
        }
        another.lazy?.let { alazy ->
            return alazy.all { this[it] }
        }
        // Check if [another] is a subset of [this]
        val adata = another.data
        val asize = another.size
        val minSize = kotlin.math.min(size, asize)
        for (i in 0..<minSize) {
            val otherWord = adata[i]
            val word = data[i]
            if ((otherWord and word.inv()) != 0L) return false
        }
        for (i in size..<asize) {
            if (adata[i] != 0L) return false
        }
        return true
    }

    fun copy(): CustomBitSet {
        lazy?.let { lazy ->
            val res = CustomBitSet()
            res.size = this.size
            res.lazy!!.addAll(lazy)
            return res
        }
        return CustomBitSet(size, data.copyOf())
    }

    val isEmpty
        get(): Boolean {
            lazy?.let { return it.isEmpty() }
            for (i in 0 until size) {
                if (data[i] != 0L) return false
            }
            return true
        }

    override fun hashCode(): Int {
        val h = hashCodeLong()
        return ((h ushr 32) xor h).toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CustomBitSet) return false
        if (lazy != null || other.lazy != null) {
            if (cardinality() != other.cardinality()) return false
            forEachBit {
                if (!other[it]) return false
            }
            return true
        }
        // Dense-dense: compare logical content. `size` is treated as an upper bound;
        // stale-high words past the lower size are required to be zero, so the result
        // is correct even if one side has stale `size`.
        val minSize = minOf(size, other.size)
        for (i in 0 until minSize) {
            if (data[i] != other.data[i]) return false
        }
        for (i in minSize until size) {
            if (data[i] != 0L) return false
        }
        for (i in minSize until other.size) {
            if (other.data[i] != 0L) return false
        }
        return true
    }

    fun hashCodeLong(): Long {
        var h = 1234L
        lazy?.let { lazy ->
            lazy.forEach { bit ->
                val i = bit ushr 6
                val datai = 1L shl (bit and 63)
                h += datai * (i + 1)
            }
            return h
        }
        for (i in size - 1 downTo 0) {
            h += data[i] * (i + 1)
        }

        return h
    }

    companion object {
        private val EMPTY = LongArray(0)

        // Create a CustomBitSet with data as the underlying storage.
        // Does not copy the data array!
        fun valueOf(data: LongArray): CustomBitSet {
            return CustomBitSet(data.size, data).also { it.shrinkDenseSize() }
        }
    }
}
