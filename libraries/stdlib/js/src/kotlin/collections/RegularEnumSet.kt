/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * [EnumSet] private implementation class, for enum types those with [Int.SIZE_BITS] or fewer elements.
 *
 * No hashCode override, use [AbstractSet.hashCode] in same consistent for other Sets.
 */
@Suppress("EqualsOrHashCode")
internal class RegularEnumSet<E : Enum<E>>(type: JsClass<E>, universe: Array<E>) : EnumSet<E>(type, universe) {

    private var bits: Int = 0
    override val size: Int get() = bits.bits()

    override fun isEmpty(): Boolean = bits == 0

    override fun contains(element: E): Boolean = bits and (1 shl element.ordinal) != 0

    override fun containsAll(elements: Collection<E>): Boolean {
        if (elements !is RegularEnumSet<E>) {
            return super.containsAll(elements)
        }

        return if (type != elements.type) {
            elements.isEmpty() // any set always contains all elements of empty set
        } else {
            0 == (elements.bits and bits.inv())
        }
    }

    override fun add(element: E): Boolean {
        val oldBits = bits
        bits = bits or (1 shl element.ordinal)
        return bits != oldBits
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if (elements !is RegularEnumSet<E>) {
            return super.addAll(elements)
        }

        return if (type != elements.type) {
            if (elements.isEmpty()) {
                false
            } else {
                throw ClassCastException("${elements.type.name} != ${type.name}")
            }
        } else {
            val oldBits = bits
            bits = bits or elements.bits
            bits != oldBits
        }
    }

    override fun remove(element: E): Boolean {
        val oldBits = bits
        bits = bits and (1 shl element.ordinal).inv()
        return bits != oldBits
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        if (elements !is RegularEnumSet<E>) {
            return super.removeAll(elements)
        }

        return if (type != elements.type) {
            false // we can not remove anything
        } else {
            val oldBits = bits
            bits = bits and elements.bits.inv()
            bits != oldBits
        }
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        if (elements !is RegularEnumSet<E>) {
            return super.retainAll(elements)
        }

        return if (type != elements.type) { // no thing contains in [elements]
            val changed = bits != 0
            bits = 0
            changed
        } else {
            val oldBits = bits
            bits = bits and elements.bits
            bits != oldBits
        }
    }

    override fun filledUp(): RegularEnumSet<E> {
        if (universe.isNotEmpty()) {
            bits = (-1) ushr -universe.size
        }

        return this
    }

    override fun clear() {
        bits = 0
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RegularEnumSet<*>) {
            return super.equals(other)
        }

        return if (type != other.type) {
            bits == 0 && 0 == other.bits
        } else {
            bits == other.bits
        }
    }

    override fun iterator(): MutableIterator<E> = BitIterator()

    private inner class BitIterator : MutableIterator<E> {

        private var unseen = bits
        private var lowBit = 0

        override fun hasNext(): Boolean = unseen != 0

        override fun next(): E {
            if (unseen == 0) {
                throw NoSuchElementException()
            }

            lowBit = unseen and -unseen
            unseen -= lowBit

            return universe[lowBit.trailingZeros()]
        }

        override fun remove() {
            if (lowBit == 0) {
                throw IllegalStateException()
            }

            bits = bits and lowBit.inv()
            lowBit = 0
        }
    }
}
