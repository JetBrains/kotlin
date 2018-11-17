/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * [EnumSet] private implementation class, for [Int.SIZE_BITS] or fewer constants enum types.
 */
internal class RegularEnumSet<E : Enum<E>>(type: JsClass<E>, universe: Array<E>) : EnumSet<E>(type, universe) {

    private var bits: Int = 0
    override val size: Int get() = bits.bits()

    override fun isEmpty(): Boolean = bits == 0

    override fun contains(element: E): Boolean = bits and (1 shl element.ordinal) != 0

    override fun containsAll(elements: Collection<E>): Boolean {
        if (elements !is RegularEnumSet<E>) {
            return super.containsAll(elements)
        }

        if (type != elements.type) {
            return elements.isEmpty() // any set always contains all elements of empty set
        }

        return (elements.bits and bits.inv()) == 0
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

        if (type != elements.type) {
            return if (elements.isEmpty()) {
                false
            } else {
                throw ClassCastException("${elements.type.name} != ${type.name}")
            }
        }

        val oldBits = bits
        bits = bits or elements.bits
        return bits != oldBits
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

        if (type != elements.type) {
            return false // we can not remove anything
        }

        val oldBits = bits
        bits = bits and elements.bits.inv()
        return bits != oldBits
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        if (elements !is RegularEnumSet<E>) {
            return super.retainAll(elements)
        }

        if (type != elements.type) { // no thing contains in [elements]
            val changed = bits != 0
            bits = 0
            return changed
        }

        val oldBits = bits
        bits = bits and elements.bits
        return bits != oldBits
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
