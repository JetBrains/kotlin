/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
@file:Suppress("DEPRECATION") // Char.toInt()
package kotlin.text.regex

/**
 * User defined character classes (e.g. [abef]).
 */
// TODO: replace the implementation with one using BitSet for first 256 symbols and a hash table / tree for the rest of UTF.
internal class CharClass(val ignoreCase: Boolean = false, negative: Boolean = false)  : AbstractCharClass()  {

    var invertedSurrogates = false

    /**
     * Shows if the alt flags was inverted during the range construction process.
     * E.g. consider the following range: [\D3]. Here we firstly add the \D char class (which has the alt flag set) into a
     * resulting char set. After that the resulting char also has the alt flag set. But then we need to add the '3' character
     * into this class with positive sense. So we set the inverted flag to show that the range is not negative ([^..])
     * by itself but was inverted during some transformations.
     */
    var inverted = false

    var hideBits = false

    internal var bits_ = BitSet()
    override val bits: BitSet?
        get() {
            if (hideBits)
                return null
            return bits_
        }

    var nonBitSet: AbstractCharClass? = null

    private val Int.asciiSupplement: Int
        get() = when {
            this in 'a'.toInt()..'z'.toInt() -> this - 32
            this in 'A'.toInt()..'Z'.toInt() -> this + 32
            else -> this
        }
    private val Int.isSurrogate: Boolean
        get() = this in Char.MIN_SURROGATE.toInt()..Char.MAX_SURROGATE.toInt()

    init {
        setNegative(negative)
    }

    /*
     * We can use this method safely even if nonBitSet != null
     * due to specific of range constructions in regular expressions.
     */
    fun add(ch: Int): CharClass {
        var character = ch
        if (ignoreCase) {
            if (character.toChar() in 'a'..'z' || character.toChar() in 'A'..'Z') {
                bits_.set(character.asciiSupplement, !inverted)
            } else if (character > 128) {
                character = Char.toLowerCase(Char.toUpperCase(character))
            }
        }
        if (character.toChar().isSurrogate()) {
            lowHighSurrogates.set(character - Char.MIN_SURROGATE.toInt(), !invertedSurrogates)
        }
        bits_.set(character, !inverted)
        if (!mayContainSupplCodepoints && Char.isSupplementaryCodePoint(ch)) {
            mayContainSupplCodepoints = true
        }
        return this
    }

    fun add(ch: Char): CharClass = add(ch.toInt())

    /*
     * The difference between add(AbstractCharClass) and union(AbstractCharClass)
     * is that add() is used for constructions like "[^abc\\d]"
     * (this pattern doesn't match "1")
     * while union is used for constructions like "[^abc[\\d]]"
     * (this pattern matches "1").
     */
    fun add(another: AbstractCharClass): CharClass {

        if (!mayContainSupplCodepoints && another.mayContainSupplCodepoints) {
            mayContainSupplCodepoints = true
        }

        // Process surrogates.
        if (!invertedSurrogates) {

            //A | !B = ! ((A ^ B) & B)
            if (another.altSurrogates) {
                lowHighSurrogates.xor(another.lowHighSurrogates)
                lowHighSurrogates.and(another.lowHighSurrogates)
                altSurrogates = !altSurrogates
                invertedSurrogates = true

                //A | B
            } else {
                lowHighSurrogates.or(another.lowHighSurrogates)
            }
        } else {

            //!A | !B = !(A & B)
            if (another.altSurrogates) {
                lowHighSurrogates.and(another.lowHighSurrogates)

                //!A | B = !(A & !B)
            } else {
                lowHighSurrogates.andNot(another.lowHighSurrogates)
            }
        }

        val anotherBits = another.bits
        if (!hideBits && anotherBits != null) {
            if (!inverted) {

                //A | !B = ! ((A ^ B) & B)
                if (another.isNegative()) {
                    bits_.xor(anotherBits)
                    bits_.and(anotherBits)
                    alt = !alt
                    inverted = true

                    //A | B
                } else {
                    bits_.or(anotherBits)
                }
            } else {

                //!A | !B = !(A & B)
                if (another.isNegative()) {
                    bits_.and(anotherBits)

                    //!A | B = !(A & !B)
                } else {
                    bits_.andNot(anotherBits)
                }
            }
        // Some of charclasses hides its bits
        } else {
            val curAlt = alt

            if (nonBitSet == null) {

                if (curAlt && !inverted && bits_.isEmpty) {
                    nonBitSet = object : AbstractCharClass() {
                        override operator fun contains(ch: Int): Boolean {
                            return another.contains(ch)
                        }
                    }
                } else {

                    /*
                     * We keep the value of alt unchanged for
                     * constructions like [^[abc]fgb] by using
                     * the formula a ^ b == !a ^ !b.
                     */
                    if (curAlt) {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return !(curAlt xor bits_.get(ch) || curAlt xor inverted xor another.contains(ch))
                            }
                        }
                    } else {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return curAlt xor bits_.get(ch) || curAlt xor inverted xor another.contains(ch)
                            }
                        }
                    }
                }

                hideBits = true
            } else {
                val nb = nonBitSet

                if (curAlt) {
                    nonBitSet = object : AbstractCharClass() {
                        override operator fun contains(ch: Int): Boolean {
                            return !(curAlt xor (nb!!.contains(ch) || another.contains(ch)))
                        }
                    }
                } else {
                    nonBitSet = object : AbstractCharClass() {
                        override operator fun contains(ch: Int): Boolean {
                            return curAlt xor (nb!!.contains(ch) || another.contains(ch))
                        }
                    }
                }
            }
        }

        return this
    }

    fun add(start: Int, end: Int): CharClass {
        if (start > end)
            throw IllegalArgumentException("Incorrect range of symbols (start > end)")
        val minSurrogate = Char.MIN_SURROGATE.toInt()
        val maxSurrogate = Char.MAX_SURROGATE.toInt()
        if (ignoreCase) {
            // TODO: Make a faster implementation.
            for (i in start..end) {
                add(i)
            }
        } else {
            // No intersection with surrogate characters.
            if (end < minSurrogate || start > maxSurrogate) {
                bits_.set(start, end + 1, !inverted)
            } else {
                val surrogatesStart = maxOf(start, minSurrogate)
                val surrogatesEnd = minOf(end, maxSurrogate)
                bits_.set(start, end + 1, !inverted)
                lowHighSurrogates.set(surrogatesStart - minSurrogate,
                        surrogatesEnd - minSurrogate + 1,
                        !invertedSurrogates)
                if (!mayContainSupplCodepoints && end >= Char.MIN_SUPPLEMENTARY_CODE_POINT) {
                    mayContainSupplCodepoints = true
                }
            }
        }
        return this
    }

    fun add(start: Char, end: Char): CharClass = add(start.toInt(), end.toInt())

    // OR operation
    fun union(another: AbstractCharClass) {
        if (!mayContainSupplCodepoints && another.mayContainSupplCodepoints) {
            mayContainSupplCodepoints = true
        }


        if (altSurrogates xor another.altSurrogates) {

            //!A | B = !(A & !B)
            if (altSurrogates) {
                lowHighSurrogates.andNot(another.lowHighSurrogates)

                //A | !B = !((A ^ B) & B)
            } else {
                lowHighSurrogates.xor(another.lowHighSurrogates)
                lowHighSurrogates.and(another.lowHighSurrogates)
                altSurrogates = true
            }

        } else {

            //!A | !B = !(A & B)
            if (altSurrogates) {
                lowHighSurrogates.and(another.lowHighSurrogates)

                //A | B
            } else {
                lowHighSurrogates.or(another.lowHighSurrogates)
            }
        }

        val anotherBits = another.bits
        if (!hideBits && anotherBits != null) {
            if (alt xor another.isNegative()) {

                //!A | B = !(A & !B)
                if (alt) {
                    bits_.andNot(anotherBits)

                    //A | !B = !((A ^ B) & B)
                } else {
                    bits_.xor(anotherBits)
                    bits_.and(anotherBits)
                    alt = true
                }

            } else {

                //!A | !B = !(A & B)
                if (alt) {
                    bits_.and(anotherBits)

                    //A | B
                } else {
                    bits_.or(anotherBits)
                }
            }
        } else {
            val curAlt = alt

            if (nonBitSet == null) {

                if (!inverted && bits_.isEmpty) {
                    if (curAlt) {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return !another.contains(ch)
                            }
                        }
                    } else {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return another.contains(ch)
                            }
                        }
                    }
                } else {

                    if (curAlt) {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return !(another.contains(ch) || curAlt xor bits_.get(ch))
                            }
                        }
                    } else {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return another.contains(ch) || curAlt xor bits_.get(ch)
                            }
                        }
                    }
                }
                hideBits = true
            } else {
                val nb = nonBitSet

                if (curAlt) {
                    nonBitSet = object : AbstractCharClass() {
                        override operator fun contains(ch: Int): Boolean {
                            return !(curAlt xor nb!!.contains(ch) || another.contains(ch))
                        }
                    }
                } else {
                    nonBitSet = object : AbstractCharClass() {
                        override operator fun contains(ch: Int): Boolean {
                            return curAlt xor nb!!.contains(ch) || another.contains(ch)
                        }
                    }
                }
            }
        }
    }

    // AND operation
    fun intersection(another: AbstractCharClass) {
        if (!mayContainSupplCodepoints && another.mayContainSupplCodepoints) {
            mayContainSupplCodepoints = true
        }

        if (altSurrogates xor another.altSurrogates) {

            //!A & B = ((A ^ B) & B)
            if (altSurrogates) {
                lowHighSurrogates.xor(another.lowHighSurrogates)
                lowHighSurrogates.and(another.lowHighSurrogates)
                altSurrogates = false

                //A & !B
            } else {
                lowHighSurrogates.andNot(another.lowHighSurrogates)
            }
        } else {

            //!A & !B = !(A | B)
            if (altSurrogates) {
                lowHighSurrogates.or(another.lowHighSurrogates)

                //A & B
            } else {
                lowHighSurrogates.and(another.lowHighSurrogates)
            }
        }

        val anotherBits = another.bits
        if (!hideBits && anotherBits != null) {

            if (alt xor another.isNegative()) {

                //!A & B = ((A ^ B) & B)
                if (alt) {
                    bits_.xor(anotherBits)
                    bits_.and(anotherBits)
                    alt = false

                    //A & !B
                } else {
                    bits_.andNot(anotherBits)
                }
            } else {

                //!A & !B = !(A | B)
                if (alt) {
                    bits_.or(anotherBits)

                    //A & B
                } else {
                    bits_.and(anotherBits)
                }
            }
        } else {
            val curAlt = alt

            if (nonBitSet == null) {

                if (!inverted && bits_.isEmpty) {
                    if (curAlt) {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return !another.contains(ch)
                            }
                        }
                    } else {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return another.contains(ch)
                            }
                        }
                    }
                } else {

                    if (curAlt) {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return !(another.contains(ch) && curAlt xor bits_.get(ch))
                            }
                        }
                    } else {
                        nonBitSet = object : AbstractCharClass() {
                            override operator fun contains(ch: Int): Boolean {
                                return another.contains(ch) && curAlt xor bits_.get(ch)
                            }
                        }
                    }
                }
                hideBits = true
            } else {
                val nb = nonBitSet

                if (curAlt) {
                    nonBitSet = object : AbstractCharClass() {
                        override operator fun contains(ch: Int): Boolean {
                            return !(curAlt xor nb!!.contains(ch) && another.contains(ch))
                        }
                    }
                } else {
                    nonBitSet = object : AbstractCharClass() {
                        override operator fun contains(ch: Int): Boolean {
                            return curAlt xor nb!!.contains(ch) && another.contains(ch)
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns `true` if character class contains symbol specified,
     * `false` otherwise. Note: #setNegative() method changes the
     * meaning of contains method;

     * @param ch
     * *
     * @return `true` if character class contains symbol specified;
     *     */
    override operator fun contains(ch: Int): Boolean {
        if (nonBitSet == null) {
            return alt xor bits_.get(ch)
        } else {
            return alt xor nonBitSet!!.contains(ch)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override val instance: AbstractCharClass
        get() {

            if (nonBitSet == null) {
                val bs = bits

                val res = object : AbstractCharClass() {
                    override operator fun contains(ch: Int): Boolean {
                        return this.alt xor bs!!.get(ch)
                    }

                    override fun toString(): String {
                        val temp = StringBuilder()
                        var i = bs!!.nextSetBit(0)
                        while (i >= 0) {
                            temp.append(Char.toChars(i))
                            temp.append('|')
                            i = bs.nextSetBit(i + 1)
                        }

                        if (temp.length > 0)
                            temp.deleteAt(temp.length - 1)

                        return temp.toString()
                    }

                }
                return res.setNegative(isNegative())
            } else {
                return this
            }
        }

    @OptIn(ExperimentalStdlibApi::class)
    //for debugging purposes only
    override fun toString(): String {
        val temp = StringBuilder()
        var i = bits_.nextSetBit(0)
        while (i >= 0) {
            temp.append(Char.toChars(i))
            temp.append('|')
            i = bits_.nextSetBit(i + 1)
        }

        if (temp.length > 0)
            temp.deleteAt(temp.length - 1)

        return temp.toString()
    }
}

