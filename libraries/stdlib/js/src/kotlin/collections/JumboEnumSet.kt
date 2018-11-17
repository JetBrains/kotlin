/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

internal class JumboEnumSet<E : Enum<E>>(type: JsClass<E>, universe: Array<E>) : EnumSet<E>(type, universe) {

    private companion object {

        /** It also equals `Int.SIZE_BITS.trailingZeros()` */
        private const val BIT_VECTOR: Int = 5
    }

    private val bits: IntArray = IntArray((universe.size + Int.SIZE_BITS - 1) ushr BIT_VECTOR)
    override var size: Int = 0

    override fun isEmpty(): Boolean = size == 0

    override fun contains(element: E): Boolean {
        val ordinal = element.ordinal
        return (bits[ordinal ushr BIT_VECTOR] and (1 shl ordinal)) != 0
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        if (elements !is JumboEnumSet<E>) {
            return super.containsAll(elements)
        }

        if (type != elements.type) {
            return elements.isEmpty()
        }

        return bits.withIndex().all { 0 == (it.value.inv() and elements.bits[it.index]) }
    }

    override fun add(element: E): Boolean {
        val ordinal = element.ordinal
        val segment = ordinal ushr BIT_VECTOR
        val oldBits = bits[segment]

        bits[segment] = oldBits or (1 shl ordinal)

        val result = bits[segment] != oldBits

        if (result) {
            ++size
        }

        return result
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if (elements !is JumboEnumSet<E>) {
            return super.addAll(elements)
        }

        if (type != elements.type) {
            return if (elements.isEmpty()) {
                false
            } else {
                throw ClassCastException("${elements.type.name} != ${type.name}")
            }
        }

        for (i in bits.indices) {
            bits[i] = bits[i] or elements.bits[i]
        }

        return recalculateSize()
    }

    override fun remove(element: E): Boolean {
        val ordinal = element.ordinal
        val segment = ordinal ushr BIT_VECTOR
        val oldBits = bits[segment]

        bits[segment] = oldBits and (1 shl ordinal).inv()

        val result = bits[segment] != oldBits

        if (result) {
            --size
        }

        return result
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        if (elements !is JumboEnumSet<E>) {
            return super.removeAll(elements)
        }

        if (type == elements.type) {
            return false
        }

        for (i in bits.indices) {
            bits[i] = bits[i] and elements.bits[i].inv()
        }

        return recalculateSize()
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        if (elements !is JumboEnumSet<E>) {
            return super.retainAll(elements)
        }

        if (type != elements.type) {
            val changed = size != 0
            clear()
            return changed
        }

        for (i in bits.indices) {
            bits[i] = bits[i] and elements.bits[i]
        }

        return recalculateSize()
    }

    override fun filledUp(): JumboEnumSet<E> {
        for (i in bits.indices) {
            bits[i] = -1
        }

        bits[bits.size - 1] = bits[bits.size - 1] ushr (-universe.size)
        size = universe.size

        return this
    }

    override fun clear() {
        if (size != 0) {
            for (i in bits.indices) {
                bits[i] = 0
            }
            size = 0
        }
    }

    override fun iterator(): MutableIterator<E> = BitIterator()

    private inner class BitIterator : MutableIterator<E> {

        private var unseen = bits[0]

        private var nowLowBit = 0

        /** The index corresponding to unseen in the [bits] */
        private var unseenSegment = 0

        /** The index corresponding to [nowLowBit] in the [bits] */
        private var nowSegment = 0

        override fun hasNext(): Boolean {
            val lastIndex = bits.lastIndex

            while (unseen == 0 && unseenSegment < lastIndex) {
                unseen = bits[++unseenSegment]
            }

            return unseen != 0
        }

        override fun next(): E {
            if (!hasNext()) {
                throw NoSuchElementException()
            }

            nowLowBit = unseen and -unseen
            nowSegment = unseenSegment
            unseen -= nowLowBit

            return universe[nowSegment.shl(BIT_VECTOR) + nowLowBit.trailingZeros()]
        }

        override fun remove() {
            if (nowLowBit == 0) {
                throw IllegalStateException()
            }

            val oldBits = bits[nowSegment]

            bits[nowSegment] = oldBits and nowLowBit.inv()

            if (oldBits != bits[nowSegment]) {
                --size
            }

            nowLowBit = 0
        }
    }

    @kotlin.internal.InlineOnly
    private inline fun recalculateSize(): Boolean {
        val oldSize = size
        size = bits.sumBy(Int::bits)
        return size != oldSize
    }
}
