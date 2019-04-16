/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

import kotlin.text.regex.*

@PublishedApi
internal interface FlagEnum {
    val value: Int
    val mask: Int
}

private fun Iterable<FlagEnum>.toInt(): Int = this.fold(0, { value, option -> value or option.value })

private fun fromInt(value: Int): Set<RegexOption> =
        RegexOption.values().filterTo(mutableSetOf<RegexOption>()) { value and it.mask == it.value  }

/**
 * Provides enumeration values to use to set regular expression options.
 */
public actual enum class RegexOption(override val value: Int, override val mask: Int = value) : FlagEnum {
    // common

    /** Enables case-insensitive matching. Case comparison is Unicode-aware. */
    IGNORE_CASE(Pattern.CASE_INSENSITIVE),

    /**
     * Enables multiline mode.
     *
     * In multiline mode the expressions `^` and `$` match just after or just before,
     * respectively, a line terminator or the end of the input sequence.
     */
    MULTILINE(Pattern.MULTILINE),

    /**
     * Enables literal parsing of the pattern.
     *
     * Metacharacters or escape sequences in the input sequence will be given no special meaning.
     */
    LITERAL(Pattern.LITERAL),

    /** Enables Unix lines mode. In this mode, only the `'\n'` is recognized as a line terminator. */
    UNIX_LINES(Pattern.UNIX_LINES),

    /** Permits whitespace and comments in pattern. */
    COMMENTS(Pattern.COMMENTS),

    /** Enables the mode, when the expression `.` matches any character, including a line terminator. */
    DOT_MATCHES_ALL(Pattern.DOTALL),

    /** Enables equivalence by canonical decomposition. */
    CANON_EQ(Pattern.CANON_EQ)
}


/**
 * Represents the results from a single capturing group within a [MatchResult] of [Regex].
 *
 * @param value The value of captured group.
 * @param range The range of indices in the input string where group was captured.
 *
 * The [range] property is available on JVM only.
 */
public actual data class MatchGroup(actual val value: String, val range: IntRange)

/**
 * Represents a compiled regular expression.
 * Provides functions to match strings in text with a pattern, replace the found occurrences and split text around matches.
 */
public actual class Regex internal constructor(internal val nativePattern: Pattern) {

    internal enum class Mode {
        FIND, MATCH
    }

    /** Creates a regular expression from the specified [pattern] string and the default options.  */
    actual constructor(pattern: String): this(Pattern(pattern))

    /** Creates a regular expression from the specified [pattern] string and the specified single [option].  */
    actual constructor(pattern: String, option: RegexOption): this(Pattern(pattern, ensureUnicodeCase(option.value)))

    /** Creates a regular expression from the specified [pattern] string and the specified set of [options].  */
    actual constructor(pattern: String, options: Set<RegexOption>): this(Pattern(pattern, ensureUnicodeCase(options.toInt())))


    /** The pattern string of this regular expression. */
    actual val pattern: String
        get() = nativePattern.pattern

    private val startNode = nativePattern.startNode

    /** The set of options that were used to create this regular expression.  */
    actual val options: Set<RegexOption> = fromInt(nativePattern.flags)

    actual companion object {
        /**
         * Returns a regular expression that matches the specified [literal] string literally.
         * No characters of that string will have special meaning when searching for an occurrence of the regular expression.
         */
        actual fun fromLiteral(literal: String): Regex = Regex(literal, RegexOption.LITERAL)

        /**
         * Returns a regular expression pattern string that matches the specified [literal] string literally.
         * No characters of that string will have special meaning when searching for an occurrence of the regular expression.
         */
        actual fun escape(literal: String): String = Pattern.quote(literal)

        /**
         * Returns a literal replacement expression for the specified [literal] string.
         * No characters of that string will have special meaning when it is used as a replacement string in [Regex.replace] function.
         */
        actual fun escapeReplacement(literal: String): String {
            if (!literal.contains('\\') && !literal.contains('$'))
                return literal

            val result = StringBuilder(literal.length * 2)
            literal.forEach {
                if (it == '\\' || it == '$') {
                    result.append('\\')
                }
                result.append(it)
            }

            return result.toString()
        }

        // TODO: Remove
        private fun ensureUnicodeCase(flags: Int) = flags
    }

    private fun doMatch(input: CharSequence, mode: Mode): MatchResult? {
        // TODO: Harmony has a default constructor for MatchResult. Do we need it?
        // TODO: Reuse the matchResult.
        val matchResult = MatchResultImpl(input, this)
        matchResult.mode = mode
        val matches = startNode.matches(0, input, matchResult) >= 0
        if (!matches) {
            return null
        }
        matchResult.finalizeMatch()
        return matchResult
    }

    /** Indicates whether the regular expression matches the entire [input]. */
    actual infix fun matches(input: CharSequence): Boolean = doMatch(input, Mode.MATCH) != null

    /** Indicates whether the regular expression can find at least one match in the specified [input]. */
    actual fun containsMatchIn(input: CharSequence): Boolean = find(input) != null

    /**
     * Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     */
    actual fun find(input: CharSequence, startIndex: Int): MatchResult? {
        if (startIndex < 0 || startIndex > input.length) {
            throw IndexOutOfBoundsException("Start index out of bounds: $startIndex")
        }
        val matchResult = MatchResultImpl(input, this)
        matchResult.mode = Mode.FIND
        matchResult.startIndex = startIndex
        val foundIndex = startNode.find(startIndex, input, matchResult)
        if (foundIndex >= 0) {
            matchResult.finalizeMatch()
            return matchResult
        } else {
            return null
        }
    }

    /**
     * Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     */
    actual fun findAll(input: CharSequence, startIndex: Int): Sequence<MatchResult>
            = generateSequence({ find(input, startIndex) }, MatchResult::next)

    /**
     * Attempts to match the entire [input] CharSequence against the pattern.
     *
     * @return An instance of [MatchResult] if the entire input matches or `null` otherwise.
     */
    actual fun matchEntire(input: CharSequence): MatchResult?= doMatch(input, Mode.MATCH)

    private fun processReplacement(match: MatchResult, replacement: String): String {
        val result = StringBuilder(replacement.length)
        var escaped = false
        var backReference = false
        for (ch in replacement) {
            when {
                escaped -> {
                    result.append(ch)
                    escaped = false
                }
                backReference -> {
                    if (ch !in '0'..'9') {
                        throw IllegalArgumentException("Incorrect back reference: $ch.")
                    }
                    val group = ch - '0'
                    result.append(match.groupValues[group])
                    // We don't catch IndexOutOfBoundException here because
                    // it's a correct exception in case of a wrong group number.
                    // TODO: But we can rethrow it with more informative message.
                    backReference = false
                }
                ch == '\\' -> escaped = true
                ch == '$' -> backReference = true
                else -> result.append(ch)
            }
        }
        if (backReference || escaped) {
            throw IllegalArgumentException("Unexpected end of replacement.")
        }
        return result.toString()
    }

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with
     * specified [replacement] expression.
     *
     * @param replacement A replacement expression that can include substitutions.
     */
    actual fun replace(input: CharSequence, replacement: String): String
            = replace(input) { match -> processReplacement(match, replacement) }

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with the result of
     * the given function [transform] that takes [MatchResult] and returns a string to be used as a
     * replacement for that match.
     */
    actual fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String {
        var match: MatchResult? = find(input) ?: return input.toString()

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
     * @param replacement A replacement expression that can include substitutions.
     */
    actual fun replaceFirst(input: CharSequence, replacement: String): String {
        val match = find(input) ?: return input.toString()
        val length = input.length
        val result = StringBuilder(length)
        result.append(input, 0, match.range.start)
        result.append(processReplacement(match, replacement))
        if (match.range.endInclusive + 1 < length) {
            result.append(input, match.range.endInclusive + 1, length)
        }
        return result.toString()
    }

    /**
     * Splits the [input] CharSequence around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     *              Zero by default means no limit is set.
     */
    actual fun split(input: CharSequence, limit: Int): List<String> {
        require(limit >= 0, { "Limit must be non-negative, but was $limit." } )

        var match: MatchResult? = find(input)

        if (match == null || limit == 1) return listOf(input.toString())

        val result = ArrayList<String>(if (limit > 0) limit.coerceAtMost(10) else 10)
        var lastStart = 0
        val lastSplit = limit - 1 // negative if there's no limit

        do {
            result.add(input.substring(lastStart, match!!.range.start))
            lastStart = match.range.endInclusive + 1
            if (lastSplit >= 0 && result.size == lastSplit) break
            match = match.next()
        } while (match != null)

        result.add(input.substring(lastStart, input.length))

        return result
    }

    /**
     * Returns the string representation of this regular expression, namely the [pattern] of this regular expression.
     *
     * Note that another regular expression constructed from the same pattern string may have different [options]
     * and may match strings differently.
     */
    override fun toString(): String = nativePattern.toString()
}
