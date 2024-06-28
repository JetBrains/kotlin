/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.text

import java.util.Collections
import java.util.EnumSet
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.internal.IMPLEMENTATIONS

private interface FlagEnum {
    public val value: Int
    public val mask: Int
}

private fun Iterable<FlagEnum>.toInt(): Int =
    this.fold(0, { value, option -> value or option.value })

private inline fun <reified T> fromInt(value: Int): Set<T> where T : FlagEnum, T : Enum<T> =
    Collections.unmodifiableSet(EnumSet.allOf(T::class.java).apply {
        retainAll { value and it.mask == it.value }
    })

/**
 * Provides enumeration values to use to set regular expression options.
 */
public actual enum class RegexOption(override val value: Int, override val mask: Int = value) : FlagEnum {
    // common

    /** Enables case-insensitive matching. Case comparison is Unicode-aware. */
    IGNORE_CASE(Pattern.CASE_INSENSITIVE),

    /** Enables multiline mode.
     *
     * In multiline mode the expressions `^` and `$` match just after or just before,
     * respectively, a line terminator or the end of the input sequence. */
    MULTILINE(Pattern.MULTILINE),

    //jvm-specific

    /** Enables literal parsing of the pattern.
     *
     * Metacharacters or escape sequences in the input sequence will be given no special meaning.
     */
    LITERAL(Pattern.LITERAL),

//    // Unicode case is enabled by default with the IGNORE_CASE
//    /** Enables Unicode-aware case folding. */
//    UNICODE_CASE(Pattern.UNICODE_CASE)

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
 */
public actual data class MatchGroup(public actual val value: String, public val range: IntRange)

/**
 * Represents a compiled regular expression.
 * Provides functions to match strings in text with a pattern, replace the found occurrences and split text around matches.
 *
 * For pattern syntax reference see [Pattern].
 */
@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS") // Counterpart for @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual class Regex
@PublishedApi
internal constructor(private val nativePattern: Pattern) : Serializable {


    /** Creates a regular expression from the specified [pattern] string and the default options.  */
    public actual constructor(pattern: String) : this(Pattern.compile(pattern))

    /** Creates a regular expression from the specified [pattern] string and the specified single [option].  */
    public actual constructor(pattern: String, option: RegexOption) : this(Pattern.compile(pattern, ensureUnicodeCase(option.value)))

    /** Creates a regular expression from the specified [pattern] string and the specified set of [options].  */
    public actual constructor(pattern: String, options: Set<RegexOption>) : this(Pattern.compile(pattern, ensureUnicodeCase(options.toInt())))


    /** The pattern string of this regular expression. */
    public actual val pattern: String
        get() = nativePattern.pattern()

    private var _options: Set<RegexOption>? = null
    /** The set of options that were used to create this regular expression.  */
    public actual val options: Set<RegexOption> get() = _options ?: fromInt<RegexOption>(nativePattern.flags()).also { _options = it }

    /** Indicates whether the regular expression matches the entire [input]. */
    public actual infix fun matches(input: CharSequence): Boolean = nativePattern.matcher(input).matches()

    /** Indicates whether the regular expression can find at least one match in the specified [input]. */
    public actual fun containsMatchIn(input: CharSequence): Boolean = nativePattern.matcher(input).find()

    /**
     * Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of the [input] char sequence.
     * @sample samples.text.Regexps.find
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual fun find(input: CharSequence, startIndex: Int = 0): MatchResult? =
        nativePattern.matcher(input).findNext(startIndex, input)

    /**
     * Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     *
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of the [input] char sequence.
     *
     * @sample samples.text.Regexps.findAll
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual fun findAll(input: CharSequence, startIndex: Int = 0): Sequence<MatchResult> {
        if (startIndex < 0 || startIndex > input.length) {
            throw IndexOutOfBoundsException("Start index out of bounds: $startIndex, input length: ${input.length}")
        }
        return generateSequence({ find(input, startIndex) }, MatchResult::next)
    }

    /**
     * Attempts to match the entire [input] CharSequence against the pattern.
     *
     * @return An instance of [MatchResult] if the entire input matches or `null` otherwise.
     */
    public actual fun matchEntire(input: CharSequence): MatchResult? = nativePattern.matcher(input).matchEntire(input)

    @SinceKotlin("1.7")
    @WasExperimental(ExperimentalStdlibApi::class)
    public actual fun matchAt(input: CharSequence, index: Int): MatchResult? =
        nativePattern.matcher(input).useAnchoringBounds(false).useTransparentBounds(true).region(index, input.length).run {
            if (lookingAt()) MatcherMatchResult(this, input) else null
        }

    @SinceKotlin("1.7")
    @WasExperimental(ExperimentalStdlibApi::class)
    public actual fun matchesAt(input: CharSequence, index: Int): Boolean =
        nativePattern.matcher(input).useAnchoringBounds(false).useTransparentBounds(true).region(index, input.length).lookingAt()

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with specified [replacement] expression.
     *
     * The replacement string may contain references to the captured groups during a match. Occurrences of `${name}` or `$index`
     * in the replacement string will be substituted with the subsequences corresponding to the captured groups with the specified name or index.
     * In case of `$index`, the first digit after '$' is always treated as a part of group reference. Subsequent digits are incorporated
     * into `index` only if they would form a valid group reference. Only the digits '0'..'9' are considered as potential components
     * of the group reference. Note that indexes of captured groups start from 1, and the group with index 0 is the whole match.
     * In case of `${name}`, the `name` can consist of latin letters 'a'..'z' and 'A'..'Z', or digits '0'..'9'. The first character must be
     * a letter.
     *
     * Backslash character '\' can be used to include the succeeding character as a literal in the replacement string, e.g, `\$` or `\\`.
     * [Regex.escapeReplacement] can be used if [replacement] have to be treated as a literal string.
     *
     * Note that named capturing groups are supported in Java 7 or later.
     *
     * @param input the char sequence to find matches of this regular expression in
     * @param replacement the expression to replace found matches with
     * @return the result of replacing each occurrence of this regular expression in [input] with the result of evaluating the [replacement] expression
     * @throws RuntimeException if [replacement] expression is malformed, or capturing group with specified `name` or `index` does not exist
     */
    public actual fun replace(input: CharSequence, replacement: String): String = nativePattern.matcher(input).replaceAll(replacement)

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with the result of
     * the given function [transform] that takes [MatchResult] and returns a string to be used as a
     * replacement for that match.
     */
    public actual fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String {
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
     * The replacement string may contain references to the captured groups during a match. Occurrences of `${name}` or `$index`
     * in the replacement string will be substituted with the subsequences corresponding to the captured groups with the specified name or index.
     * In case of `$index`, the first digit after '$' is always treated as a part of group reference. Subsequent digits are incorporated
     * into `index` only if they would form a valid group reference. Only the digits '0'..'9' are considered as potential components
     * of the group reference. Note that indexes of captured groups start from 1, and the group with index 0 is the whole match.
     * In case of `${name}`, the `name` can consist of latin letters 'a'..'z' and 'A'..'Z', or digits '0'..'9'. The first character must be
     * a letter.
     *
     * Backslash character '\' can be used to include the succeeding character as a literal in the replacement string, e.g, `\$` or `\\`.
     * [Regex.escapeReplacement] can be used if [replacement] have to be treated as a literal string.
     *
     * Note that named capturing groups are supported in Java 7 or later.
     *
     * @param input the char sequence to find a match of this regular expression in
     * @param replacement the expression to replace the found match with
     * @return the result of replacing the first occurrence of this regular expression in [input] with the result of evaluating the [replacement] expression
     * @throws RuntimeException if [replacement] expression is malformed, or capturing group with specified `name` or `index` does not exist
     */
    public actual fun replaceFirst(input: CharSequence, replacement: String): String =
        nativePattern.matcher(input).replaceFirst(replacement)


    /**
     * Splits the [input] CharSequence to a list of strings around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual fun split(input: CharSequence, limit: Int = 0): List<String> {
        requireNonNegativeLimit(limit)

        val matcher = nativePattern.matcher(input)
        if (limit == 1 || !matcher.find()) return listOf(input.toString())

        val result = ArrayList<String>(if (limit > 0) limit.coerceAtMost(10) else 10)
        var lastStart = 0
        val lastSplit = limit - 1 // negative if there's no limit

        do {
            result.add(input.substring(lastStart, matcher.start()))
            lastStart = matcher.end()
            if (lastSplit >= 0 && result.size == lastSplit) break
        } while (matcher.find())

        result.add(input.substring(lastStart, input.length))

        return result
    }

    /**
     * Splits the [input] CharSequence to a sequence of strings around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     * @sample samples.text.Regexps.splitToSequence
     */
    @SinceKotlin("1.6")
    @WasExperimental(ExperimentalStdlibApi::class)
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual fun splitToSequence(input: CharSequence, limit: Int = 0): Sequence<String> {
        requireNonNegativeLimit(limit)

        return sequence {
            val matcher = nativePattern.matcher(input)
            if (limit == 1 || !matcher.find()) {
                yield(input.toString())
                return@sequence
            }

            var nextStart = 0
            var splitCount = 0

            do {
                yield(input.substring(nextStart, matcher.start()))
                nextStart = matcher.end()
            } while (++splitCount != limit - 1 && matcher.find())

            yield(input.substring(nextStart, input.length))
        }
    }

    /**
     * Returns the string representation of this regular expression, namely the [pattern] of this regular expression.
     *
     * Note that another regular expression constructed from the same pattern string may have different [options]
     * and may match strings differently.
     */
    public override fun toString(): String = nativePattern.toString()

    /**
     * Returns an instance of [Pattern] with the same pattern string and options as this instance of [Regex] has.
     *
     * Provides the way to use [Regex] where [Pattern] is required.
     */
    public fun toPattern(): Pattern = nativePattern

    private fun writeReplace(): Any = Serialized(nativePattern.pattern(), nativePattern.flags())

    private class Serialized(val pattern: String, val flags: Int) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 0L
        }

        private fun readResolve(): Any = Regex(Pattern.compile(pattern, flags))
    }

    public actual companion object {
        /**
         * Returns a regular expression that matches the specified [literal] string literally.
         * No characters of that string will have special meaning when searching for an occurrence of the regular expression.
         */
        public actual fun fromLiteral(literal: String): Regex = literal.toRegex(RegexOption.LITERAL)

        /**
         * Returns a regular expression pattern string that matches the specified [literal] string literally.
         * No characters of that string will have special meaning when searching for an occurrence of the regular expression.
         */
        public actual fun escape(literal: String): String = Pattern.quote(literal)

        /**
         * Returns a literal replacement expression for the specified [literal] string.
         * No characters of that string will have special meaning when it is used as a replacement string in [Regex.replace] function.
         */
        public actual fun escapeReplacement(literal: String): String = Matcher.quoteReplacement(literal)

        private fun ensureUnicodeCase(flags: Int) = if (flags and Pattern.CASE_INSENSITIVE != 0) flags or Pattern.UNICODE_CASE else flags
    }

}

// implementation

private fun Matcher.findNext(from: Int, input: CharSequence): MatchResult? {
    return if (!find(from)) null else MatcherMatchResult(this, input)
}

private fun Matcher.matchEntire(input: CharSequence): MatchResult? {
    return if (!matches()) null else MatcherMatchResult(this, input)
}

private class MatcherMatchResult(private val matcher: Matcher, private val input: CharSequence) : MatchResult {
    private val matchResult: java.util.regex.MatchResult get() = matcher
    override val range: IntRange
        get() = matchResult.range()
    override val value: String
        get() = matchResult.group()

    override val groups: MatchGroupCollection = object : MatchNamedGroupCollection, AbstractCollection<MatchGroup?>() {
        override val size: Int get() = matchResult.groupCount() + 1
        override fun isEmpty(): Boolean = false

        override fun iterator(): Iterator<MatchGroup?> = indices.asSequence().map { this[it] }.iterator()
        override fun get(index: Int): MatchGroup? {
            val range = matchResult.range(index)
            return if (range.start >= 0)
                MatchGroup(matchResult.group(index), range)
            else
                null
        }

        override fun get(name: String): MatchGroup? {
            return IMPLEMENTATIONS.getMatchResultNamedGroup(matchResult, name)
        }
    }

    private var groupValues_: List<String>? = null

    override val groupValues: List<String>
        get() {
            if (groupValues_ == null) {
                groupValues_ = object : AbstractList<String>() {
                    override val size: Int get() = matchResult.groupCount() + 1
                    override fun get(index: Int): String = matchResult.group(index) ?: ""
                }
            }
            return groupValues_!!
        }

    override fun next(): MatchResult? {
        val nextIndex = matchResult.end() + if (matchResult.end() == matchResult.start()) 1 else 0
        return if (nextIndex <= input.length) matcher.pattern().matcher(input).findNext(nextIndex, input) else null
    }
}


private fun java.util.regex.MatchResult.range(): IntRange = start() until end()
private fun java.util.regex.MatchResult.range(groupIndex: Int): IntRange = start(groupIndex) until end(groupIndex)
