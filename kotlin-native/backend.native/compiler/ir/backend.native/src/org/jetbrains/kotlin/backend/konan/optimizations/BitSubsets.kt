/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

class BitSubsets<T> {
    private val elementIndex = mutableMapOf<T, Int>()
    private val elements = mutableListOf<T>()

    fun register(element: T): Int {
        return elementIndex.getOrPut(element) {
            elements.add(element)
            elements.size - 1
        }
    }

    inner class Subset() : Set<T> {
        private val bitset = java.util.BitSet()

        constructor(elements: List<T>) : this() {
            for (element in elements) {
                this.bitset.set(register(element))
            }
        }

        constructor(vararg elements: T) : this(elements.toList())

        private constructor(bitset: java.util.BitSet) : this() {
            this.bitset.or(bitset)
        }

        override val size: Int
            get() = bitset.cardinality()

        override fun isEmpty(): Boolean = size == 0

        override fun contains(element: T): Boolean {
            val bitIndex = elementIndex[element] ?: return false
            return bitset.get(bitIndex)
        }

        override fun iterator(): Iterator<T> = object : Iterator<T> {
            var nextSetBit = bitset.nextSetBit(0)
            override fun hasNext(): Boolean = nextSetBit >= 0
            override fun next(): T = elements[nextSetBit].also {
                nextSetBit = bitset.nextSetBit(nextSetBit + 1)
            }
        }

        override fun containsAll(elements: Collection<T>): Boolean = when (elements) {
            is Subset -> (elements union this) == this
            else -> elements.all { it in this }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BitSubsets<*>.Subset) return false
            return this.bitset == other.bitset
        }

        override fun hashCode(): Int = bitset.hashCode()

        operator fun plus(element: T): Subset {
            val bitIndex = register(element)
            if (bitset.get(bitIndex)) return this
            return Subset(bitset).apply {
                bitset.set(bitIndex)
            }
        }

        operator fun minus(element: T): Subset {
            val bitIndex = register(element)
            if (!bitset.get(bitIndex)) return this
            return Subset(bitset).apply {
                bitset.clear(bitIndex)
            }
        }

        infix fun union(other: Subset): Subset {
            if (this == other) return this
            return Subset(bitset).apply {
                bitset.or(other.bitset)
            }
        }

        infix fun intersect(other: Subset): Subset {
            if (this == other) return this
            return Subset(bitset).apply {
                bitset.and(other.bitset)
            }
        }
    }
}

class BitSubsetsUnionSemilattice<T>(subsets: BitSubsets<T>) : Semilattice<BitSubsets<T>.Subset> {
    override val top: BitSubsets<T>.Subset = subsets.Subset()
    override fun meet(x: BitSubsets<T>.Subset, y: BitSubsets<T>.Subset): BitSubsets<T>.Subset = x union y
}

fun <T> BitSubsets<T>.unionSemilattice() = BitSubsetsUnionSemilattice(this)
