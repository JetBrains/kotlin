/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * [EnumSet] private implementation class, for 32 or fewer constants enum types.
 */
internal class RegularEnumSet<E : Enum<E>>(clazz: JsClass<E>, universe: Array<E>) : EnumSet<E>(clazz, universe) {

    private var bits: Int = 0

    override val size: Int get() = bits.bits()

    override fun isEmpty(): Boolean = bits == 0

    override fun contains(element: E): Boolean = bits and (1 shl element.ordinal) != 0

    override fun containsAll(elements: Collection<E>): Boolean {
        if (elements !is RegularEnumSet<E>) {
            return elements.all(::contains)
        }

        if (clazz != elements.clazz) {
            return elements.isEmpty() // any set always contains all elements of empty set
        }

        return (elements.bits and bits.inv()) == 0
    }

    override fun iterator(): MutableIterator<E> = BitIterator()

    override fun add(element: E): Boolean {
        val oldBits = bits
        bits = bits or (1 shl element.ordinal)
        return bits != oldBits
    }

    override fun addAll() {
        if (universe.isNotEmpty()) {
            bits = (-1) ushr (-universe.size)
        }
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if (elements !is RegularEnumSet<E>) {
            var modified = false

            for (e in elements) {
                if (add(e)) {
                    modified = true
                }
            }

            return modified
        }

        if (clazz != elements.clazz) {
            return if (elements.isEmpty()) {
                false
            } else {
                throw ClassCastException("${elements.clazz.name} != ${clazz.name}")
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
            return (this as MutableIterable<E>).removeAll { it in elements }
        }

        if (clazz != elements.clazz) {
            return false // we can not remove anything
        }

        val oldBits = bits
        bits = bits and elements.bits.inv()
        return bits != oldBits
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        if (elements !is RegularEnumSet<E>) {
            return (this as MutableIterable<E>).removeAll { it !in elements }
        }

        if (clazz != elements.clazz) { // no thing contains in [elements]
            val changed = bits != 0
            bits = 0
            return changed
        }

        val oldBits = bits
        bits = bits and elements.bits
        return bits != oldBits
    }

    override fun clear() {
        bits = 0
    }

    private inner class BitIterator : MutableIterator<E> {

        private var unseen = bits
        private var last = 0

        override fun hasNext(): Boolean = unseen != 0

        override fun next(): E {
            if (unseen == 0) {
                throw NoSuchElementException()
            }

            last = unseen and -unseen // lowest one-bit (low bit)
            unseen -= last

            return universe[last.trailingZeros()]
        }

        override fun remove() {
            if (last == 0) {
                throw IllegalStateException()
            }

            bits = bits and last.inv()
            last = 0
        }
    }
}
