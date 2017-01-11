/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.text

header interface Appendable {
    fun append(c: Char): Appendable
    fun append(csq: CharSequence?): Appendable
    fun append(csq: CharSequence?, start: Int, end: Int): Appendable
}

header class StringBuilder : Appendable, CharSequence {
    constructor()
    constructor(capacity: Int)
    constructor(seq: CharSequence)

    override val length: Int
    override operator fun get(index: Int): Char
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    fun reverse(): StringBuilder
    override fun append(c: Char): StringBuilder
    override fun append(csq: CharSequence?): StringBuilder
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder
    fun append(obj: Any?): StringBuilder
}

header class Regex {
    constructor(pattern: String)
    constructor(pattern: String, option: RegexOption)
    constructor(pattern: String, options: Set<RegexOption>)

    fun matchEntire(input: CharSequence): MatchResult?
    fun matches(input: CharSequence): Boolean
    fun containsMatchIn(input: CharSequence): Boolean
    fun replace(input: CharSequence, replacement: String): String
    fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String
    fun replaceFirst(input: CharSequence, replacement: String): String

    // TODO: requires optional parameters
    fun find(input: CharSequence): MatchResult?
    fun find(input: CharSequence, startIndex: Int): MatchResult?
    fun findAll(input: CharSequence): Sequence<MatchResult>
    fun findAll(input: CharSequence, startIndex: Int): Sequence<MatchResult>
    fun split(input: CharSequence): List<String>
    fun split(input: CharSequence, limit: Int): List<String>

    header companion object {
        fun fromLiteral(literal: String): Regex
        fun escape(literal: String): String
        fun escapeReplacement(literal: String): String
    }
}

header class MatchGroup {
    val value: String
}

header enum class RegexOption {
    IGNORE_CASE,
    MULTILINE
}


// From char.kt

header fun Char.isWhitespace(): Boolean
header fun Char.toLowerCase(): Char
header fun Char.toUpperCase(): Char
header fun Char.isHighSurrogate(): Boolean
header fun Char.isLowSurrogate(): Boolean

// From string.kt

internal header fun String.nativeIndexOf(str: String, fromIndex: Int): Int
internal header fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int
internal header fun String.nativeStartsWith(s: String, position: Int): Boolean
internal header fun String.nativeEndsWith(s: String): Boolean


public header fun String.substring(startIndex: Int): String
public header fun String.substring(startIndex: Int, endIndex: Int): String


public header inline fun String.toUpperCase(): String
public header inline fun String.toLowerCase(): String
public header inline fun String.capitalize(): String
public header inline fun String.decapitalize(): String
public header fun CharSequence.repeat(n: Int): String


// TOOD: requires optional parameters (and named!)
header fun String.replace(oldChar: Char, newChar: Char): String
header fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean): String
header fun String.replace(oldValue: String, newValue: String): String
header fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean): String
header fun String.replaceFirst(oldChar: Char, newChar: Char): String
header fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean): String
header fun String.replaceFirst(oldValue: String, newValue: String): String
header fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean): String
header fun String?.equals(other: String?): Boolean
header fun String?.equals(other: String?, ignoreCase: Boolean): Boolean

// From stringsCode.kt

internal inline header fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int
internal inline header fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int

header fun CharSequence.isBlank(): Boolean
header fun CharSequence.regionMatches(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean): Boolean
