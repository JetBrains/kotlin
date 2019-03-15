/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.js.RegExp

/**
 * Provides enumeration values to use to set regular expression options.
 */
public actual enum class RegexOption(val value: String) {
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
public actual data class MatchGroup(actual val value: String)


/**
 * Represents a compiled regular expression.
 * Provides functions to match strings in text with a pattern, replace the found occurrences and split text around matches.
 *
 * For pattern syntax reference see [MDN RegExp](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp#Special_characters_meaning_in_regular_expressions)
 * and [http://www.w3schools.com/jsref/jsref_obj_regexp.asp](https://www.w3schools.com/jsref/jsref_obj_regexp.asp).
 *
 * @constructor Creates a regular expression from the specified [pattern] string and the specified set of [options].
 */
public actual class Regex actual constructor(pattern: String, options: Set<RegexOption>) {

    /** Creates a regular expression from the specified [pattern] string and the specified single [option].  */
    public actual constructor(pattern: String, option: RegexOption) : this(pattern, setOf(option))

    /** Creates a regular expression from the specified [pattern] string and the default options.  */
    public actual constructor(pattern: String) : this(pattern, emptySet())


    /** The pattern string of this regular expression. */
    public actual val pattern: String = pattern
    /** The set of options that were used to create this regular expression. */
    public actual val options: Set<RegexOption> = options.toSet()
    private val nativePattern: RegExp = RegExp(pattern, options.map { it.value }.joinToString(separator = "") + "g")

    /** Indicates whether the regular expression matches the entire [input]. */
    public actual infix fun matches(input: CharSequence): Boolean {
        nativePattern.reset()
        val match = nativePattern.exec(input.toString())
        return match != null && match.index == 0 && nativePattern.lastIndex == input.length
    }

    /** Indicates whether the regular expression can find at least one match in the specified [input]. */
    public actual fun containsMatchIn(input: CharSequence): Boolean {
        nativePattern.reset()
        return nativePattern.test(input.toString())
    }

    /** Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual fun find(input: CharSequence, startIndex: Int = 0): MatchResult? = nativePattern.findNext(input.toString(), startIndex)

    /**
     * Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     *
     * @sample samples.text.Regexps.findAll
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual fun findAll(input: CharSequence, startIndex: Int = 0): Sequence<MatchResult> =
        generateSequence({ find(input, startIndex) }, { match -> match.next() })

    /**
     * Attempts to match the entire [input] CharSequence against the pattern.
     *
     * @return An instance of [MatchResult] if the entire input matches or `null` otherwise.
     */
    public actual fun matchEntire(input: CharSequence): MatchResult? {
        if (pattern.startsWith('^') && pattern.endsWith('$'))
            return find(input)
        else
            return Regex("^${pattern.trimStart('^').trimEnd('$')}$", options).find(input)
    }

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with specified [replacement] expression.
     *
     * @param replacement A replacement expression that can include substitutions. See [String.prototype.replace](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/replace) function docs for details.
     */
    public actual fun replace(input: CharSequence, replacement: String): String = input.toString().nativeReplace(nativePattern, replacement)

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with the result of
     * the given function [transform] that takes [MatchResult] and returns a string to be used as a
     * replacement for that match.
     */
    public actual inline fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String {
        var match = find(input)
        if (match == null) return input.toString()

        var lastStart = 0
        val length = input.length
        val sb = StringBuilder(length)
        do {
            val foundMatch = match!!
            sb.append(input, lastStart, foundMatch.range.start)
            sb.append(transform(foundMatch))
            lastStart = foundMatch.range.endInclusive + 1
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
     * @param replacement A replacement expression that can include substitutions. See [String.prototype.replace](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/replace) function docs for details.
     */
    public actual fun replaceFirst(input: CharSequence, replacement: String): String {
        val nonGlobalOptions = options.map { it.value }.joinToString(separator = "")
        return input.toString().nativeReplace(RegExp(pattern, nonGlobalOptions), replacement)
    }

    /**
     * Splits the [input] CharSequence around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual fun split(input: CharSequence, limit: Int = 0): List<String> {
        require(limit >= 0) { "Limit must be non-negative, but was $limit" }
        val matches = findAll(input).let { if (limit == 0) it else it.take(limit - 1) }
        val result = mutableListOf<String>()
        var lastStart = 0

        for (match in matches) {
            result.add(input.subSequence(lastStart, match.range.start).toString())
            lastStart = match.range.endInclusive + 1
        }
        result.add(input.subSequence(lastStart, input.length).toString())
        return result
    }


    /**
     * Returns the string representation of this regular expression, namely the [pattern] of this regular expression.
     *
     * Note that another regular expression constructed from the same pattern string may have different [options]
     * and may match strings differently.
     */
    public override fun toString(): String = nativePattern.toString()

    actual companion object {
        /**
         * Returns a regular expression that matches the specified [literal] string literally.
         * No characters of that string will have special meaning when searching for an occurrence of the regular expression.
         */
        public actual fun fromLiteral(literal: String): Regex = Regex(escape(literal))

        /**
         * Returns a regular expression pattern string that matches the specified [literal] string literally.
         * No characters of that string will have special meaning when searching for an occurrence of the regular expression.
         */
        public actual fun escape(literal: String): String = literal.nativeReplace(patternEscape, "\\$&")

        /**
         * Returns a literal replacement expression for the specified [literal] string.
         * No characters of that string will have special meaning when it is used as a replacement string in [Regex.replace] function.
         */
        public actual fun escapeReplacement(literal: String): String = literal.nativeReplace(replacementEscape, "$$$$")

        private val patternEscape = RegExp("""[-\\^$*+?.()|[\]{}]""", "g")
        private val replacementEscape = RegExp("""\$""", "g")
    }
}

/** Creates a regular expression from the specified [pattern] string and the specified single [option].  */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
@JsName("Regex_sb3q2\$")
public fun Regex_0(pattern: String, option: RegexOption): Regex = Regex(pattern, setOf(option))

/** Creates a regular expression from the specified [pattern] string and the default options.  */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
@JsName("Regex_61zpoe\$")
public fun Regex_1(pattern: String): Regex = Regex(pattern, emptySet())




private fun RegExp.findNext(input: String, from: Int): MatchResult? {
    this.lastIndex = from
    val match = exec(input)
    if (match == null) return null
    val range = match.index..lastIndex - 1

    return object : MatchResult {
        override val range: IntRange = range
        override val value: String
            get() = match[0]!!

        override val groups: MatchGroupCollection = object : MatchGroupCollection, AbstractCollection<MatchGroup?>() {
            override val size: Int get() = match.length
            override fun iterator(): Iterator<MatchGroup?> = indices.asSequence().map { this[it] }.iterator()
            override fun get(index: Int): MatchGroup? = match[index]?.let { MatchGroup(it) }
        }


        private var groupValues_: List<String>? = null

        override val groupValues: List<String>
            get() {
                if (groupValues_ == null) {
                    groupValues_ = object : AbstractList<String>() {
                        override val size: Int get() = match.length
                        override fun get(index: Int): String = match[index] ?: ""
                    }
                }
                return groupValues_!!
            }

        override fun next(): MatchResult? = this@findNext.findNext(input, if (range.isEmpty()) range.start + 1 else range.endInclusive + 1)
    }
}
