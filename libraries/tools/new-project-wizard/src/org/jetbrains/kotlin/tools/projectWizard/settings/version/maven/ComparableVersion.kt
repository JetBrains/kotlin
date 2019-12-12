/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

@file:Suppress("NAME_SHADOWING")

package org.jetbrains.kotlin.tools.projectWizard.settings.version.maven

import org.jetbrains.kotlin.tools.projectWizard.settings.version.maven.ComparableVersion.Item.Companion.BIGINTEGER_ITEM
import org.jetbrains.kotlin.tools.projectWizard.settings.version.maven.ComparableVersion.Item.Companion.INT_ITEM
import org.jetbrains.kotlin.tools.projectWizard.settings.version.maven.ComparableVersion.Item.Companion.LIST_ITEM
import org.jetbrains.kotlin.tools.projectWizard.settings.version.maven.ComparableVersion.Item.Companion.LONG_ITEM
import org.jetbrains.kotlin.tools.projectWizard.settings.version.maven.ComparableVersion.Item.Companion.STRING_ITEM
import java.math.BigInteger
import java.util.*


/**
 * Converted to Kotlin version of `org.apache.maven.artifact.versioning.ComparableVersion`
 *
 * Generic implementation of version comparison.
 *
 *
 *
 * Features:
 *
 *  * mixing of '`-`' (hyphen) and '`.`' (dot) separators,
 *  * transition between characters and digits also constitutes a separator:
 * `1.0alpha1 => [1, 0, alpha, 1]`
 *  * unlimited number of version components,
 *  * version components in the text can be digits or strings,
 *  * strings are checked for well-known qualifiers and the qualifier ordering is used for version ordering.
 * Well-known qualifiers (case insensitive) are:
 *  * `alpha` or `a`
 *  * `beta` or `b`
 *  * `milestone` or `m`
 *  * `rc` or `cr`
 *  * `snapshot`
 *  * `(the empty string)` or `ga` or `final`
 *  * `sp`
 *
 * Unknown qualifiers are considered after known qualifiers, with lexical order (always case insensitive),
 *
 *  * a hyphen usually precedes a qualifier, and is always less important than something preceded with a dot.
 *
 *
 * @author [Kenney Westerhof](mailto:kenney@apache.org)
 * @author [Hervé Boutemy](mailto:hboutemy@apache.org)
 * @see ["Versioning" on Maven Wiki](https://cwiki.apache.org/confluence/display/MAVENOLD/Versioning)
 */
class ComparableVersion(version: String) : Comparable<ComparableVersion?> {
    private var value: String? = null
    var canonical: String? = null
        get() {
            if (field == null) {
                field = items.toString()
            }
            return field
        }
        private set
    private var items: ListItem? = null

    private interface Item {
        operator fun compareTo(item: Item?): Int
        val type: Int
        val isNull: Boolean

        companion object {
            const val INT_ITEM = 3
            const val LONG_ITEM = 4
            const val BIGINTEGER_ITEM = 0
            const val STRING_ITEM = 1
            const val LIST_ITEM = 2
        }
    }

    /**
     * Represents a numeric item in the version item list that can be represented with an int.
     */
    private class IntItem : Item {
        private val value: Int

        private constructor() {
            value = 0
        }

        internal constructor(str: String) {
            value = str.toInt()
        }

        override val type: Int
            get() = INT_ITEM

        override val isNull: Boolean
            get() = value == 0

        override fun compareTo(item: Item?): Int {
            return if (item == null) {
                if (value == 0) 0 else 1 // 1.0 == 1, 1.1 > 1
            } else when (item.type) {
                INT_ITEM -> {
                    val itemValue = (item as IntItem).value
                    if (value < itemValue) -1 else if (value == itemValue) 0 else 1
                }
                LONG_ITEM, BIGINTEGER_ITEM -> -1
                STRING_ITEM -> 1 // 1.1 > 1-sp
                LIST_ITEM -> 1 // 1.1 > 1-1
                else -> throw IllegalStateException("invalid item: " + item.javaClass)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null || javaClass != other.javaClass) {
                return false
            }
            val intItem = other as IntItem
            return value == intItem.value
        }

        override fun hashCode(): Int {
            return value
        }

        override fun toString(): String {
            return value.toString()
        }

        companion object {
            val ZERO = IntItem()
        }
    }

    /**
     * Represents a numeric item in the version item list that can be represented with a long.
     */
    private class LongItem internal constructor(str: String) :
        Item {
        private val value = str.toLong()
        override val type: Int
            get() = LONG_ITEM

        override val isNull: Boolean
            get() = value == 0L

        override fun compareTo(item: Item?): Int {
            return if (item == null) {
                if (value == 0L) 0 else 1 // 1.0 == 1, 1.1 > 1
            } else when (item.type) {
                INT_ITEM -> 1
                LONG_ITEM -> {
                    val itemValue = (item as LongItem).value
                    if (value < itemValue) -1 else if (value == itemValue) 0 else 1
                }
                BIGINTEGER_ITEM -> -1
                STRING_ITEM -> 1 // 1.1 > 1-sp


                LIST_ITEM -> 1 // 1.1 > 1-1


                else -> throw IllegalStateException("invalid item: " + item.javaClass)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null || javaClass != other.javaClass) {
                return false
            }
            val longItem = other as LongItem
            return value == longItem.value
        }

        override fun hashCode(): Int {
            return (value xor (value ushr 32)).toInt()
        }

        override fun toString(): String {
            return value.toString()
        }

    }

    /**
     * Represents a numeric item in the version item list.
     */
    private class BigIntegerItem internal constructor(str: String?) :
        Item {
        private val value = BigInteger(str)
        override val type: Int
            get() = BIGINTEGER_ITEM

        override val isNull: Boolean
            get() = BigInteger.ZERO == value

        override fun compareTo(item: Item?): Int {
            return if (item == null) {
                if (BigInteger.ZERO == value) 0 else 1 // 1.0 == 1, 1.1 > 1
            } else when (item.type) {
                INT_ITEM, LONG_ITEM -> 1
                BIGINTEGER_ITEM -> value.compareTo((item as BigIntegerItem).value)
                STRING_ITEM -> 1 // 1.1 > 1-sp
                LIST_ITEM -> 1 // 1.1 > 1-1
                else -> throw IllegalStateException("invalid item: " + item.javaClass)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null || javaClass != other.javaClass) {
                return false
            }
            val that = other as BigIntegerItem
            return value == that.value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

        override fun toString(): String {
            return value.toString()
        }

    }

    /**
     * Represents a string in the version item list, usually a qualifier.
     */
    private class StringItem internal constructor(value: String, followedByDigit: Boolean) :
        Item {
        companion object {
            private val QUALIFIERS = listOf("alpha", "beta", "milestone", "rc", "snapshot", "", "sp")
            private val ALIASES = Properties()
            /**
             * A comparable value for the empty-string qualifier. This one is used to determine if a given qualifier makes
             * the version older than one without a qualifier, or more recent.
             */
            private val RELEASE_VERSION_INDEX = QUALIFIERS.indexOf("").toString()

            /**
             * Returns a comparable value for a qualifier.
             *
             *
             * This method takes into account the ordering of known qualifiers then unknown qualifiers with lexical
             * ordering.
             *
             *
             * just returning an Integer with the index here is faster, but requires a lot of if/then/else to check for -1
             * or QUALIFIERS.size and then resort to lexical ordering. Most comparisons are decided by the first character,
             * so this is still fast. If more characters are needed then it requires a lexical sort anyway.
             *
             * @param qualifier
             * @return an equivalent value that can be used with lexical comparison
             */
            fun comparableQualifier(qualifier: String): String {
                val i = QUALIFIERS.indexOf(qualifier)
                return if (i == -1) QUALIFIERS.size.toString() + "-" + qualifier else i.toString()
            }

            init {
                ALIASES["ga"] = ""
                ALIASES["final"] = ""
                ALIASES["release"] = ""
                ALIASES["cr"] = "rc"
            }
        }

        private val value: String
        override val type: Int
            get() = STRING_ITEM

        override val isNull: Boolean
            get() = comparableQualifier(
                value
            ).compareTo(RELEASE_VERSION_INDEX) == 0

        override fun compareTo(item: Item?): Int {
            return if (item == null) {
                // 1-rc < 1, 1-ga > 1

                comparableQualifier(
                    value
                )
                    .compareTo(RELEASE_VERSION_INDEX)
            } else when (item.type) {
                INT_ITEM, LONG_ITEM, BIGINTEGER_ITEM -> -1 // 1.any < 1.1 ?
                STRING_ITEM -> comparableQualifier(
                    value
                ).compareTo(
                    comparableQualifier(
                        (item as StringItem).value
                    )
                )
                LIST_ITEM -> -1 // 1.any < 1-1
                else -> throw IllegalStateException("invalid item: " + item.javaClass)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null || javaClass != other.javaClass) {
                return false
            }
            val that = other as StringItem
            return value == that.value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

        override fun toString(): String {
            return value
        }

        init {
            var value = value
            if (followedByDigit && value.length == 1) {
                // a1 = alpha-1, b1 = beta-1, m1 = milestone-1
                when (value[0]) {
                    'a' -> value = "alpha"
                    'b' -> value = "beta"
                    'm' -> value = "milestone"
                    else -> {
                    }
                }
            }
            this.value = ALIASES.getProperty(value, value)
        }
    }

    /**
     * Represents a version list item. This class is used both for the global item list and for sub-lists (which start
     * with '-(number)' in the version specification).
     */
    private class ListItem : ArrayList<Item?>(),
        Item {
        override val type: Int
            get() = LIST_ITEM

        override val isNull: Boolean
            get() = size == 0

        internal fun normalize() {
            for (i in size - 1 downTo 0) {
                val lastItem = get(i)
                if (lastItem!!.isNull) {
                    // remove null trailing items: 0, "", empty list
                    removeAt(i)
                } else if (lastItem !is ListItem) {
                    break
                }
            }
        }

        override fun compareTo(item: Item?): Int {
            if (item == null) {
                if (size == 0) {
                    return 0 // 1-0 = 1- (normalize) = 1
                }
                val first = get(0)
                return first!!.compareTo(null)
            }
            when (item.type) {
                INT_ITEM, LONG_ITEM, BIGINTEGER_ITEM -> return -1 // 1-1 < 1.0.x
                STRING_ITEM -> return 1 // 1-1 > 1-sp
                LIST_ITEM -> {
                    val left: Iterator<Item?> = iterator()
                    val right: Iterator<Item?> = (item as ListItem).iterator()
                    while (left.hasNext() || right.hasNext()) {
                        val l = if (left.hasNext()) left.next() else null
                        val r = if (right.hasNext()) right.next() else null

                        // if this is shorter, then invert the compare and mul with -1
                        val result = l?.compareTo(r) ?: if (r == null) 0 else -1 * r.compareTo(l)
                        if (result != 0) {
                            return result
                        }
                    }
                    return 0
                }
                else -> throw IllegalStateException("invalid item: " + item.javaClass)
            }
        }

        override fun toString(): String {
            val buffer = StringBuilder()
            for (item in this) {
                if (buffer.length > 0) {
                    buffer.append(if (item is ListItem) '-' else '.')
                }
                buffer.append(item)
            }
            return buffer.toString()
        }
    }

    private fun parseVersion(version: String) {
        var version = version
        value = version
        items = ListItem()
        version = version.toLowerCase(Locale.ENGLISH)
        var list = items!!
        val stack: Deque<Item> = ArrayDeque()
        stack.push(list)
        var isDigit = false
        var startIndex = 0
        for (i in version.indices) {
            val c = version[i]
            when {
                c == '.' -> {
                    if (i == startIndex) {
                        list.add(IntItem.ZERO)
                    } else {
                        list.add(
                            parseItem(
                                isDigit,
                                version.substring(startIndex, i)
                            )
                        )
                    }
                    startIndex = i + 1
                }
                c == '-' -> {
                    if (i == startIndex) {
                        list.add(IntItem.ZERO)
                    } else {
                        list.add(
                            parseItem(
                                isDigit,
                                version.substring(startIndex, i)
                            )
                        )
                    }
                    startIndex = i + 1
                    list.add(ListItem().also { list = it })
                    stack.push(list)
                }
                Character.isDigit(c) -> {
                    if (!isDigit && i > startIndex) {
                        list.add(
                            StringItem(
                                version.substring(startIndex, i),
                                true
                            )
                        )
                        startIndex = i
                        list.add(ListItem().also { list = it })
                        stack.push(list)
                    }
                    isDigit = true
                }
                else -> {
                    if (isDigit && i > startIndex) {
                        list.add(
                            parseItem(
                                true,
                                version.substring(startIndex, i)
                            )
                        )
                        startIndex = i
                        list.add(ListItem().also { list = it })
                        stack.push(list)
                    }
                    isDigit = false
                }
            }
        }
        if (version.length > startIndex) {
            list.add(
                parseItem(
                    isDigit,
                    version.substring(startIndex)
                )
            )
        }
        while (!stack.isEmpty()) {
            list = stack.pop() as ListItem
            list.normalize()
        }
    }

    override operator fun compareTo(other: ComparableVersion?): Int {
        return items!!.compareTo(other?.items)
    }

    override fun toString(): String {
        return value!!
    }

    override fun equals(other: Any?): Boolean {
        return other is ComparableVersion && items == other.items
    }

    override fun hashCode(): Int {
        return items.hashCode()
    }

    // CHECKSTYLE_OFF: LineLength

    companion object {
        private const val MAX_INTITEM_LENGTH = 9
        private const val MAX_LONGITEM_LENGTH = 18
        private fun parseItem(isDigit: Boolean, buf: String): Item {
            var buf = buf
            if (isDigit) {
                buf =
                    stripLeadingZeroes(
                        buf
                    )
                if (buf.length <= MAX_INTITEM_LENGTH) {
                    // lower than 2^31

                    return IntItem(buf)
                } else if (buf.length <= MAX_LONGITEM_LENGTH) {
                    // lower than 2^63

                    return LongItem(
                        buf
                    )
                }
                return BigIntegerItem(
                    buf
                )
            }
            return StringItem(
                buf,
                false
            )
        }

        private fun stripLeadingZeroes(buf: String?): String {
            if (buf == null || buf.isEmpty()) {
                return "0"
            }
            for (i in buf.indices) {
                val c = buf[i]
                if (c != '0') {
                    return buf.substring(i)
                }
            }
            return buf
        }
    }

    init {
        parseVersion(version)
    }
}