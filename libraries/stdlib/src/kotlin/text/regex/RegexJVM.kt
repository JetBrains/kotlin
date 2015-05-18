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

import java.util.regex.Pattern
import java.util.regex.Matcher

private interface FlagEnum {
    public val value: Int
    public val mask: Int
}
private fun Iterable<FlagEnum>.toInt(): Int =
        this.fold(0, { value, option -> value or option.value })
private fun <T: FlagEnum> fromInt(value: Int, allValues: Array<T>): Set<T> =
        allValues.filter({ value and it.mask == it.value }).toSet()

/**
 * Provides enumeration values to use to set regular expression options.
 */
public enum class RegexOption(override val value: Int, override val mask: Int = value) : FlagEnum {
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

    /** Enables Unix lines mode.
     * In this mode, only the `'\n'` is recognized as a line terminator.
     */
    UNIX_LINES(Pattern.UNIX_LINES), // TODO: Remove this

    /** Permits whitespace and comments in pattern. */
    COMMENTS(Pattern.COMMENTS),

    /** Enables the mode, when the expression `.` matches any character,
     * including a line terminator.
     */
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
 * The [range] property is available on JVM only
 */
public data class MatchGroup(public val value: String, public val range: IntRange)

/**
 * Represents an immutable regular expression.
 *
 * For pattern syntax reference see [java.util.regex.Pattern]
 */
public class Regex internal (private val nativePattern: Pattern) {


    /** Creates a regular expression from the specified [pattern] string and the default options.  */
    public constructor(pattern: String): this(Pattern.compile(pattern))

    /** Creates a regular expression from the specified [pattern] string and the specified single [option].  */
    public constructor(pattern: String, option: RegexOption): this(Pattern.compile(pattern, ensureUnicodeCase(option.value)))

    /** Creates a regular expression from the specified [pattern] string and the specified set of [options].  */
    public constructor(pattern: String, options: Set<RegexOption>): this(Pattern.compile(pattern, ensureUnicodeCase(options.toInt())))


    /** The pattern string of this regular expression. */
    public val pattern: String
        get() = nativePattern.pattern()

    /** The set of options that were used to create this regular expression.  */
    public val options: Set<RegexOption> = fromInt(nativePattern.flags(), RegexOption.values())

    deprecated("To get the Matcher from java.util.regex.Pattern use toPattern to convert string to Pattern, or migrate to Regex API")
    public fun matcher(input: CharSequence): Matcher = nativePattern.matcher(input)

    /** Indicates whether the regular expression matches the entire [input]. */
    public fun matches(input: CharSequence): Boolean = nativePattern.matcher(input).matches()

    /** Indicates whether the regular expression can find at least a match in the specified [input]. */
    public fun hasMatch(input: CharSequence): Boolean = nativePattern.matcher(input).find()

    /**
     * Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     */
    public fun match(input: CharSequence, startIndex: Int = 0): MatchResult? = nativePattern.matcher(input).findNext(startIndex)

    /**
     * Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     */
    public fun matchAll(input: CharSequence, startIndex: Int = 0): Sequence<MatchResult> = sequence({ match(input, startIndex) }, { match -> match.next() })

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with specified [replacement] expression.
     *
     * @param replacement A replacement expression that can include substitutions. See [Matcher.appendReplacement] for details.
     */
    public fun replace(input: CharSequence, replacement: String): String = nativePattern.matcher(input).replaceAll(replacement)

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with the result of
     * the given function [transform] that takes [MatchResult] and returns a string to be used as a
     * replacement for that match.
     */
    public inline fun replace(input: CharSequence, transform: (MatchResult) -> String): String {
        var match = match(input)
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
    public fun replaceFirst(input: CharSequence, replacement: String): String = nativePattern.matcher(input).replaceFirst(replacement)


    /**
     * Splits this string around matches of the given regular expression.
     *
     * @param limit The maximum number of times the split can occur.
     */
    public fun split(input: CharSequence, limit: Int = 0): List<String> {
        require(limit >= 0, { "Limit must be non-negative, but was $limit" } )
        return nativePattern.split(input, if (limit == 0) -1 else limit).asList()
    }

    /** Returns the string representation of this regular expression, namely the [pattern] of this regular expression. */
    public override fun toString(): String = nativePattern.toString()

    /**
     * Returns an instance of [Pattern] with the same pattern string and options as this instance of [Regex] has.
     *
     * Provides the way to use [Regex] where [Pattern] is required.
     */
    public fun toPattern(): Pattern = nativePattern

    companion object {
        /** Returns a literal regex for the specified [literal] string. */
        public fun fromLiteral(literal: String): Regex = literal.toRegex(RegexOption.LITERAL)
        /** Returns a literal pattern for the specified [literal] string. */
        public fun escape(literal: String): String = Pattern.quote(literal)
        /** Returns a literal replacement expression for the specified [literal] string. */
        public fun escapeReplacement(literal: String): String = Matcher.quoteReplacement(literal)

        private fun ensureUnicodeCase(flags: Int) = if (flags and Pattern.CASE_INSENSITIVE != 0) flags or Pattern.UNICODE_CASE else flags
    }

}

/**
 * Converts this [Pattern] to an instance of [Regex].
 *
 * Provides the way to use Regex API on the instances of [Pattern].
 */
public fun Pattern.toRegex(): Regex = Regex(this)


// implementation

private fun Matcher.findNext(from: Int): MatchResult? {
    if (!find(from))
        return null

    val matchResult = this.toMatchResult()

    return object: MatchResult {
        override val range: IntRange
            get() = matchResult.range()
        override val value: String
            get() = matchResult.group()

        override val groups: MatchGroupCollection = object : MatchGroupCollection {
            override fun size(): Int = matchResult.groupCount() + 1
            override fun isEmpty(): Boolean = false
            override fun contains(o: Any?): Boolean = o is MatchGroup? && this.any({ it == o })
            override fun containsAll(c: Collection<Any?>): Boolean = c.all({contains(it)})

            override fun iterator(): Iterator<MatchGroup?> = indices.sequence().map { this[it] }.iterator()
            override fun get(index: Int): MatchGroup? {
                val range = matchResult.range(index)
                return if (range.start >= 0)
                    MatchGroup(matchResult.group(index), range)
                else
                    null
            }
        }


        override fun next(): MatchResult? = this@findNext.findNext(matchResult.end() + if (matchResult.end() == matchResult.start()) 1 else 0)
    }
}

private fun java.util.regex.MatchResult.range(): IntRange = start()..end()-1
private fun java.util.regex.MatchResult.range(groupIndex: Int): IntRange = start(groupIndex)..end(groupIndex)-1
