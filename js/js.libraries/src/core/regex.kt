/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import kotlin.text.js.*
import java.util.ArrayList

/**
 * Provides enumeration values to use to set regular expression options.
 */
public enum class RegexOption(val value: String) {
    /** Enables case-insensitive matching. */
    IGNORE_CASE("i"),
    /** Enables multiline mode.
     *
     * In multiline mode the expressions `^` and `$` match just after or just before,
     * respectively, a line terminator or the end of the input sequence. */
    MULTILINE("m")
}


/**
 * Represents the results from a single capturing group within a [MatchResult] of [Regex].
 *
 * @param value The value of captured group.
 */
public data class MatchGroup(val value: String)

/** A compiled representation of a regular expression.
 *
 * For pattern syntax reference see [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp] and [http://www.w3schools.com/jsref/jsref_obj_regexp.asp]
 */
public class Regex(pattern: String, options: Set<RegexOption>) {

    /** The pattern string of this regular expression. */
    public val pattern: String = pattern
    /** The set of options that were used to create this regular expression. */
    public val options: Set<RegexOption> = options.toSet()
    private val nativePattern: RegExp = RegExp(pattern, options.map { it.value }.joinToString(separator = "") + "g")

    /** Indicates whether the regular expression matches the entire [input]. */
    public fun matches(input: CharSequence): Boolean {
        nativePattern.reset()
        val match = nativePattern.exec(input.toString())
        return match != null && (match as RegExpMatch).index == 0 && nativePattern.lastIndex == input.length()
    }

    /** Indicates whether the regular expression can find at least one match in the specified [input]. */
    public fun containsMatchIn(input: CharSequence): Boolean {
        nativePattern.reset()
        return nativePattern.test(input.toString())
    }

    @Deprecated("Use containsMatchIn() or 'in' operator instead.", ReplaceWith("this in input"))
    public fun hasMatch(input: CharSequence): Boolean = containsMatchIn(input)

    /** Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     */
    public fun find(input: CharSequence, startIndex: Int = 0): MatchResult? = nativePattern.findNext(input.toString(), startIndex)

    @Deprecated("Use find() instead.", ReplaceWith("find(input, startIndex)"))
    public fun match(input: CharSequence, startIndex: Int = 0): MatchResult? = find(input, startIndex)

    /** Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     */
    public fun findAll(input: CharSequence, startIndex: Int = 0): Sequence<MatchResult> = sequence({ find(input, startIndex) }, { match -> match.next() })

    @Deprecated("Use findAll() instead.", ReplaceWith("findAll(input, startIndex)"))
    public fun matchAll(input: CharSequence, startIndex: Int = 0): Sequence<MatchResult> = findAll(input, startIndex)

    /**
     * Attempts to match the entire [input] CharSequence against the pattern.
     *
     * @return An instance of [MatchResult] if the entire input matches or `null` otherwise.
     */
    public fun matchEntire(input: CharSequence): MatchResult? {
        if (pattern.startsWith('^') && pattern.endsWith('$'))
            return find(input)
        else
            return Regex("^${pattern.trimStart('^').trimEnd('$')}$", options).find(input)
    }

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with specified [replacement] expression.
     *
     * @param replacement A replacement expression that can include substitutions. See [https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/replace] for details.
     */
    public fun replace(input: CharSequence, replacement: String): String = input.toString().nativeReplace(nativePattern, replacement)

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with the result of
     * the given function [transform] that takes [MatchResult] and returns a string to be used as a
     * replacement for that match.
     */
    public inline fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String {
        var match = find(input)
        if (match == null) return input.toString()

        var lastStart = 0
        val length = input.length()
        val sb = StringBuilder(length)
        do {
            val foundMatch = match!!
            sb.append(input, lastStart, foundMatch.range.start)
            sb.append(transform(foundMatch))
            lastStart = foundMatch.range.end + 1
            match = foundMatch.next()
        } while (lastStart < length && match != null)

        if (lastStart < length) {
            sb.append(input, lastStart, length)
        }

        return sb.toString()
    }

    /**
     * Replaces the first occurrence of this regular expression in the specified [input] string with specified [replacement] expression.
     *
     * @param replacement A replacement expression that can include substitutions. See [Matcher.appendReplacement] for details.
     */
    public fun replaceFirst(input: CharSequence, replacement: String): String {
        val nonGlobalOptions = options.map { it.value }.joinToString(separator = "")
        return input.toString().nativeReplace(RegExp(pattern, nonGlobalOptions), replacement)
    }

    /**
     * Splits this string around matches of the given regular expression.
     *
     * @param limit The maximum number of times the split can occur.
     */
    public fun split(input: CharSequence, limit: Int = 0): List<String> {
        require(limit >= 0, { "Limit must be non-negative, but was $limit" } )
        val matches = findAll(input).let { if (limit == 0) it else it.take(limit - 1) }
        val result = ArrayList<String>()
        var lastStart = 0

        for (match in matches) {
            result.add(input.subSequence(lastStart, match.range.start).toString())
            lastStart = match.range.end + 1
        }
        result.add(input.subSequence(lastStart, input.length()).toString())
        return result
    }

    /** Returns the string representation of this regular expression. */
    public override fun toString(): String = nativePattern.toString()

    companion object {
        /** Returns a literal regex for the specified [literal] string. */
        public fun fromLiteral(literal: String): Regex = Regex(escape(literal))
        /** Returns a literal pattern for the specified [literal] string. */
        public fun escape(literal: String): String = literal.nativeReplace(patternEscape, "\\$&")
        /** Returns a literal replacement exression for the specified [literal] string. */
        public fun escapeReplacement(literal: String): String = literal.nativeReplace(replacementEscape, "$$$$")

        private val patternEscape = RegExp("""[-\\^$*+?.()|[\]{}]""", "g")
        private val replacementEscape = RegExp("""\$""", "g")
    }
}

/** Creates a regular expression from the specified [pattern] string and the specified single [option].  */
public fun Regex(pattern: String, option: RegexOption): Regex = Regex(pattern, setOf(option))

/** Creates a regular expression from the specified [pattern] string and the default options.  */
public fun Regex(pattern: String): Regex = Regex(pattern, emptySet())




private fun RegExp.findNext(input: String, from: Int): MatchResult? {
    this.lastIndex = from
    val match = exec(input)
    if (match == null) return null
    val reMatch = match as RegExpMatch
    val range = reMatch.index..lastIndex-1

    return object : MatchResult {
        override val range: IntRange = range
        override val value: String
            get() = match[0]!!

        override val groups: MatchGroupCollection = object : MatchGroupCollection {
            override val size: Int get() = match.size()
            override fun isEmpty(): Boolean = size() == 0

            override fun contains(o: MatchGroup?): Boolean = this.any { it == o }
            override fun containsAll(c: Collection<MatchGroup?>): Boolean = c.all({contains(it)})

            override fun iterator(): Iterator<MatchGroup?> = indices.asSequence().map { this[it] }.iterator()

            override fun get(index: Int): MatchGroup? = match[index]?.let { MatchGroup(it) }
        }

        override fun next(): MatchResult? = this@findNext.findNext(input, if (range.isEmpty()) range.start + 1 else range.end + 1)
    }
}