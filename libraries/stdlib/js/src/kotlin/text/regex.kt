/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

private fun Iterable<RegexOption>.toFlags(prepend: String): String = joinToString("", prefix = prepend) { it.value }


/**
 * Represents the results from a single capturing group within a [MatchResult] of [Regex].
 *
 * @param value The value of captured group.
 */
public actual data class MatchGroup(actual val value: String)


/**
 * Returns a named group with the specified [name].
 *
 * @return An instance of [MatchGroup] if the group with the specified [name] was matched or `null` otherwise.
 * @throws IllegalArgumentException if there is no group with the specified [name] defined in the regex pattern.
 * @throws UnsupportedOperationException if this match group collection doesn't support getting match groups by name,
 * for example, when it's not supported by the current platform.
 */
@SinceKotlin("1.7")
public actual operator fun MatchGroupCollection.get(name: String): MatchGroup? {
    val namedGroups = this as? MatchNamedGroupCollection
        ?: throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")

    return namedGroups[name]
}


/**
 * Represents a compiled regular expression.
 * Provides functions to match strings in text with a pattern, replace the found occurrences and split text around matches.
 *
 * For pattern syntax reference see [MDN RegExp](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp#Special_characters_meaning_in_regular_expressions)
 * and [http://www.w3schools.com/jsref/jsref_obj_regexp.asp](https://www.w3schools.com/jsref/jsref_obj_regexp.asp).
 *
 * Note that `RegExp` objects under the hood are constructed with [the "u" flag](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/unicode)
 * that enables Unicode-related features in regular expressions. This also makes the pattern syntax more strict,
 * for example, prohibiting unnecessary escape sequences.
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
    private val nativePattern: RegExp = RegExp(pattern, options.toFlags("gu"))
    private var nativeStickyPattern: RegExp? = null
    private fun initStickyPattern(): RegExp =
        nativeStickyPattern ?: RegExp(pattern, options.toFlags("yu")).also { nativeStickyPattern = it }

    private var nativeMatchesEntirePattern: RegExp? = null
    private fun initMatchesEntirePattern(): RegExp =
        nativeMatchesEntirePattern ?: run {
            if (pattern.startsWith('^') && pattern.endsWith('$'))
                nativePattern
            else
                return RegExp("^${pattern.trimStart('^').trimEnd('$')}$", options.toFlags("gu"))
        }.also { nativeMatchesEntirePattern = it }


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

    @SinceKotlin("1.7")
    @WasExperimental(ExperimentalStdlibApi::class)
    public actual fun matchesAt(input: CharSequence, index: Int): Boolean {
        if (index < 0 || index > input.length) {
            throw IndexOutOfBoundsException("index out of bounds: $index, input length: ${input.length}")
        }
        val pattern = initStickyPattern()
        pattern.lastIndex = index
        return pattern.test(input.toString())
    }

    /**
     * Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of the [input] char sequence.
     * @sample samples.text.Regexps.find
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual fun find(input: CharSequence, startIndex: Int = 0): MatchResult? {
        if (startIndex < 0 || startIndex > input.length) {
            throw IndexOutOfBoundsException("Start index out of bounds: $startIndex, input length: ${input.length}")
        }
        return nativePattern.findNext(input.toString(), startIndex, nativePattern)
    }

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
        return generateSequence({ find(input, startIndex) }, { match -> match.next() })
    }

    /**
     * Attempts to match the entire [input] CharSequence against the pattern.
     *
     * @return An instance of [MatchResult] if the entire input matches or `null` otherwise.
     */
    public actual fun matchEntire(input: CharSequence): MatchResult? =
        initMatchesEntirePattern().findNext(input.toString(), 0, nativePattern)

    @SinceKotlin("1.7")
    @WasExperimental(ExperimentalStdlibApi::class)
    public actual fun matchAt(input: CharSequence, index: Int): MatchResult? {
        if (index < 0 || index > input.length) {
            throw IndexOutOfBoundsException("index out of bounds: $index, input length: ${input.length}")
        }
        return initStickyPattern().findNext(input.toString(), index, nativePattern)
    }


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
     * @param input the char sequence to find matches of this regular expression in
     * @param replacement the expression to replace found matches with
     * @return the result of replacing each occurrence of this regular expression in [input] with the result of evaluating the [replacement] expression
     * @throws RuntimeException if [replacement] expression is malformed, or capturing group with specified `name` or `index` does not exist
     */
    public actual fun replace(input: CharSequence, replacement: String): String {
        if (!replacement.contains('\\') && !replacement.contains('$')) {
            return input.toString().nativeReplace(nativePattern, replacement)
        }
        return replace(input) { substituteGroupRefs(it, replacement) }
    }

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with the result of
     * the given function [transform] that takes [MatchResult] and returns a string to be used as a
     * replacement for that match.
     */
    public actual fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String {
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
     * @param input the char sequence to find a match of this regular expression in
     * @param replacement the expression to replace the found match with
     * @return the result of replacing the first occurrence of this regular expression in [input] with the result of evaluating the [replacement] expression
     * @throws RuntimeException if [replacement] expression is malformed, or capturing group with specified `name` or `index` does not exist
     */
    public actual fun replaceFirst(input: CharSequence, replacement: String): String {
        if (!replacement.contains('\\') && !replacement.contains('$')) {
            val nonGlobalOptions = options.toFlags("u")
            return input.toString().nativeReplace(RegExp(pattern, nonGlobalOptions), replacement)
        }

        val match = find(input) ?: return input.toString()

        return buildString {
            append(input.substring(0, match.range.first))
            append(substituteGroupRefs(match, replacement))
            append(input.substring(match.range.last + 1, input.length))
        }
    }

    /**
     * Splits the [input] CharSequence to a list of strings around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual fun split(input: CharSequence, limit: Int = 0): List<String> {
        requireNonNegativeLimit(limit)
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
            var match = find(input)
            if (match == null || limit == 1) {
                yield(input.toString())
                return@sequence
            }

            var nextStart = 0
            var splitCount = 0

            do {
                val foundMatch = match!!
                yield(input.substring(nextStart, foundMatch.range.first))
                nextStart = foundMatch.range.endInclusive + 1
                match = foundMatch.next()
            } while (++splitCount != limit - 1 && match != null)

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
        public actual fun escapeReplacement(literal: String): String = literal.nativeReplace(replacementEscape, "\\$&")

        private val patternEscape = RegExp("""[\\^$*+?.()|[\]{}]""", "g")
        private val replacementEscape = RegExp("""[\\$]""", "g")

        internal fun nativeEscapeReplacement(literal: String): String = literal.nativeReplace(nativeReplacementEscape, "$$$$")
        private val nativeReplacementEscape = RegExp("""\$""", "g")
    }
}



private fun RegExp.findNext(input: String, from: Int, nextPattern: RegExp): MatchResult? {
    this.lastIndex = from
    val match = exec(input)
    if (match == null) return null
    val range = match.index..lastIndex - 1

    return object : MatchResult {
        override val range: IntRange = range
        override val value: String
            get() = match[0]!!

        override val groups: MatchGroupCollection = object : MatchNamedGroupCollection, AbstractCollection<MatchGroup?>() {
            override val size: Int get() = match.length
            override fun iterator(): Iterator<MatchGroup?> = indices.asSequence().map { this[it] }.iterator()
            override fun get(index: Int): MatchGroup? = match[index]?.let { MatchGroup(it) }

            override fun get(name: String): MatchGroup? {
                // An object of named capturing groups whose keys are the names and values are the capturing groups
                // or undefined if no named capturing groups were defined.
                val groups = match.asDynamic().groups
                    ?: throw IllegalArgumentException("Capturing group with name {$name} does not exist. No named capturing group was defined in Regex")

                // If the match was successful but the group specified failed to match any part of the input sequence,
                // the associated value is 'undefined'. Value for a non-existent key is also 'undefined'. Thus, explicitly check if the key exists.
                if (!hasOwnPrototypeProperty(groups, name))
                    throw IllegalArgumentException("Capturing group with name {$name} does not exist")

                val value = groups[name]
                return if (value == undefined) null else MatchGroup(value as String)
            }
        }

        private fun hasOwnPrototypeProperty(o: Any?, name: String): Boolean {
            return js("Object").prototype.hasOwnProperty.call(o, name).unsafeCast<Boolean>()
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

        override fun next(): MatchResult? =
            nextPattern.findNext(input, if (range.isEmpty()) advanceToNextCharacter(range.start) else range.endInclusive + 1, nextPattern)

        private fun advanceToNextCharacter(index: Int): Int {
            if (index < input.lastIndex) {
                val code1 = input.asDynamic().charCodeAt(index).unsafeCast<Int>()
                if (code1 in 0xD800..0xDBFF) {
                    val code2 = input.asDynamic().charCodeAt(index + 1).unsafeCast<Int>()
                    if (code2 in 0xDC00..0xDFFF) {
                        return index + 2
                    }
                }
            }
            return index + 1
        }
    }
}

// The same code from K/N Regex.kt
private fun substituteGroupRefs(match: MatchResult, replacement: String): String {
    var index = 0
    val result = StringBuilder()

    while (index < replacement.length) {
        val char = replacement[index++]
        if (char == '\\') {
            if (index == replacement.length)
                throw IllegalArgumentException("The Char to be escaped is missing")

            result.append(replacement[index++])
        } else if (char == '$') {
            if (index == replacement.length)
                throw IllegalArgumentException("Capturing group index is missing")

            if (replacement[index] == '{') {
                val endIndex = replacement.readGroupName(++index)

                if (index == endIndex)
                    throw IllegalArgumentException("Named capturing group reference should have a non-empty name")
                if (endIndex == replacement.length || replacement[endIndex] != '}')
                    throw IllegalArgumentException("Named capturing group reference is missing trailing '}'")

                val groupName = replacement.substring(index, endIndex)

                result.append(match.groups[groupName]?.value ?: "")
                index = endIndex + 1    // skip past '}'
            } else {
                if (replacement[index] !in '0'..'9')
                    throw IllegalArgumentException("Invalid capturing group reference")

                val groups = match.groups
                val endIndex = replacement.readGroupIndex(index, groups.size)
                val groupIndex = replacement.substring(index, endIndex).toInt()

                if (groupIndex >= groups.size)
                    throw IndexOutOfBoundsException("Group with index $groupIndex does not exist")

                result.append(groups[groupIndex]?.value ?: "")
                index = endIndex
            }
        } else {
            result.append(char)
        }
    }
    return result.toString()
}

// The name must be a legal JavaScript identifier. See https://262.ecma-international.org/5.1/#sec-7.6
// Don't try to validate the referenced group name as it may be time-consuming.
// If the name is invalid, it won't be found in `match.groups` anyway and will throw.
// Group names in the target Regex are validated at creation time.
private fun String.readGroupName(startIndex: Int): Int {
    var index = startIndex
    while (index < length) {
        if (this[index] == '}') {
            break
        } else {
            index++
        }
    }
    return index
}

private fun String.readGroupIndex(startIndex: Int, groupCount: Int): Int {
    // at least one digit after '$' is always captured
    var index = startIndex + 1
    var groupIndex = this[startIndex] - '0'

    // capture the largest valid group index
    while (index < length && this[index] in '0'..'9') {
        val newGroupIndex = (groupIndex * 10) + (this[index] - '0')
        if (newGroupIndex in 0 until groupCount) {
            groupIndex = newGroupIndex
            index++
        } else {
            break
        }
    }
    return index
}