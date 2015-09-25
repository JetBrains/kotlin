@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin

import java.io.StringReader
import java.util.regex.MatchResult
import java.util.regex.Pattern
import java.nio.charset.Charset
import java.util.*
import kotlin.text.Regex


/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified offset.
 */
internal fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int = (this as java.lang.String).indexOf(ch.toInt(), fromIndex)

/**
 * Returns the index within this string of the first occurrence of the specified substring, starting from the specified offset.
 */
internal fun String.nativeIndexOf(str: String, fromIndex: Int): Int = (this as java.lang.String).indexOf(str, fromIndex)

/**
 * Returns the index within this string of the last occurrence of the specified character.
 */
internal fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int = (this as java.lang.String).lastIndexOf(ch.toInt(), fromIndex)

/**
 * Returns the index within this string of the last occurrence of the specified character, starting from the specified offset.
 */
internal fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int = (this as java.lang.String).lastIndexOf(str, fromIndex)


/**
 * Compares this string to another string, ignoring case considerations.
 */
@Deprecated("Use equals(anotherString, ignoreCase = true) instead", ReplaceWith("equals(anotherString, ignoreCase = true)"))
public fun String.equalsIgnoreCase(anotherString: String): Boolean = equals(anotherString, ignoreCase = true)

/**
 * Returns `true` if this string is equal to [anotherString], optionally ignoring character case.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
public fun String.equals(anotherString: String, ignoreCase: Boolean = false): Boolean {
    return if (!ignoreCase)
        (this as java.lang.String).equals(anotherString)
    else
        (this as java.lang.String).equalsIgnoreCase(anotherString)
}

/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 */
public fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String {
    if (!ignoreCase)
        return (this as java.lang.String).replace(oldChar, newChar)
    else
        return splitToSequence(oldChar, ignoreCase = ignoreCase).joinToString(separator = newChar.toString())
}

/**
 * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
public fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
        splitToSequence(oldValue, ignoreCase = ignoreCase).joinToString(separator = newValue)


/**
 * Returns a new string with the first occurrence of [oldChar] replaced with [newChar].
 */
public fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String {
    val index = indexOf(oldChar, ignoreCase = ignoreCase)
    return if (index < 0) this else this.replaceRange(index, index + 1, newChar.toString())
}

/**
 * Returns a new string obtained by replacing the first occurrence of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
public fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean = false): String {
    val index = indexOf(oldValue, ignoreCase = ignoreCase)
    return if (index < 0) this else this.replaceRange(index, index + oldValue.length(), newValue)
}

@Deprecated("Use String.replaceFirst instead.", ReplaceWith("replaceFirst(oldValue, newValue, ignoreCase = ignoreCase)"))
public fun String.replaceFirstLiteral(oldValue: String, newValue: String, ignoreCase: Boolean = false): String = replaceFirst(oldValue, newValue, ignoreCase = ignoreCase)

/**
 * Returns a new string obtained by replacing each substring of this string that matches the given regular expression
 * with the given [replacement].
 */
@Deprecated("Use String.replace(Regex, String) instead. You can convert regex parameter with .toRegex() extension function.", ReplaceWith("replace(regex.toRegex(), replacement)"))
public fun String.replaceAll(regex: String, replacement: String): String = (this as java.lang.String).replaceAll(regex, replacement)

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 */
public fun String.toUpperCase(): String = (this as java.lang.String).toUpperCase()

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 */
public fun String.toLowerCase(): String = (this as java.lang.String).toLowerCase()

/**
 * Returns a new character array containing the characters from this string.
 */
public fun String.toCharArray(): CharArray = (this as java.lang.String).toCharArray()

/**
 * Uses this string as a format string and returns a string obtained by substituting the specified arguments,
 * using the default locale.
 */
public fun String.format(vararg args: Any?): String = java.lang.String.format(this, *args)

/**
 * Uses this string as a format string and returns a string obtained by substituting the specified arguments, using
 * the specified locale.
 */
public fun String.format(locale: Locale, vararg args : Any?) : String = java.lang.String.format(locale, this, *args)

/**
 * Splits this string around matches of the given regular expression.

 * @param limit Non-negative value specifying the maximum number of substrings to return.
 * Zero by default means no limit is set.
 */
public fun String.split(regex: Pattern, limit: Int = 0): List<String>
{
    require(limit >= 0, { "Limit must be non-negative, but was $limit" } )
    return regex.split(this, if (limit == 0) -1 else limit).asList()
}

/**
 * Returns a substring of this string starting with the specified index.
 */
public fun String.substring(beginIndex: Int): String = (this as java.lang.String).substring(beginIndex)

/**
 * Returns the substring of this string starting and ending at the specified indices.
 */
public fun String.substring(beginIndex: Int, endIndex: Int): String = (this as java.lang.String).substring(beginIndex, endIndex)

/**
 * Returns `true` if this string starts with the specified prefix.
 */
public fun String.startsWith(prefix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return (this as java.lang.String).startsWith(prefix)
    else
        return regionMatches(0, prefix, 0, prefix.length(), ignoreCase)
}

/**
 * Returns `true` if a substring of this string starting at the specified offset [thisOffset] starts with the specified prefix.
 */
public fun String.startsWith(prefix: String, thisOffset: Int, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return (this as java.lang.String).startsWith(prefix, thisOffset)
    else
        return regionMatches(thisOffset, prefix, 0, prefix.length(), ignoreCase)
}

/**
 * Returns `true` if this string ends with the specified suffix.
 */
public fun String.endsWith(suffix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return (this as java.lang.String).endsWith(suffix)
    else
        return regionMatches(length() - suffix.length(), suffix, 0, suffix.length(), ignoreCase = true)
}

// "constructors" for String

/**
 * Converts the data from a portion of the specified array of bytes to characters using the specified character set
 * and returns the conversion result as a string.
 *
 * @param bytes the source array for the conversion.
 * @param offset the offset in the array of the data to be converted.
 * @param length the number of bytes to be converted.
 * @param charsetName the name of the character set to use.
 */
public fun String(bytes: ByteArray, offset: Int, length: Int, charsetName: String): String = java.lang.String(bytes, offset, length, charsetName) as String

/**
 * Converts the data from a portion of the specified array of bytes to characters using the specified character set
 * and returns the conversion result as a string.
 *
 * @param bytes the source array for the conversion.
 * @param offset the offset in the array of the data to be converted.
 * @param length the number of bytes to be converted.
 * @param charset the character set to use.
 */
public fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String = java.lang.String(bytes, offset, length, charset) as String

/**
 * Converts the data from the specified array of bytes to characters using the specified character set
 * and returns the conversion result as a string.
 */
public fun String(bytes: ByteArray, charsetName: String): String = java.lang.String(bytes, charsetName) as String

/**
 * Converts the data from the specified array of bytes to characters using the specified character set
 * and returns the conversion result as a string.
 */
public fun String(bytes: ByteArray, charset: Charset): String = java.lang.String(bytes, charset) as String

/**
 * Converts the data from a portion of the specified array of bytes to characters using the UTF-8 character set
 * and returns the conversion result as a string.
 *
 * @param bytes the source array for the conversion.
 * @param offset the offset in the array of the data to be converted.
 * @param length the number of bytes to be converted.
 */
public fun String(bytes: ByteArray, offset: Int, length: Int): String = java.lang.String(bytes, offset, length, Charsets.UTF_8) as String

/**
 * Converts the data from the specified array of bytes to characters using the UTF-8 character set
 * and returns the conversion result as a string.
 */
public fun String(bytes: ByteArray): String = java.lang.String(bytes, Charsets.UTF_8) as String

/**
 * Converts the characters in the specified array to a string.
 */
public fun String(chars: CharArray): String = java.lang.String(chars) as String

/**
 * Converts the characters from a portion of the specified array to a string.
 */
public fun String(chars: CharArray, offset: Int, length: Int): String = java.lang.String(chars, offset, length) as String

/**
 * Converts the code points from a portion of the specified Unicode code point array to a string.
 */
public fun String(codePoints: IntArray, offset: Int, length: Int): String = java.lang.String(codePoints, offset, length) as String

/**
 * Converts the contents of the specified StringBuffer to a string.
 */
public fun String(stringBuffer: java.lang.StringBuffer): String = java.lang.String(stringBuffer) as String

/**
 * Converts the contents of the specified StringBuilder to a string.
 */
public fun String(stringBuilder: java.lang.StringBuilder): String = java.lang.String(stringBuilder) as String

///**
// * Replaces the first substring of this string that matches the given regular expression with the given replacement.
// */
//deprecated("Use replaceFirst(Regex, String) or replaceFirstLiteral(String, String) instead.", ReplaceWith("replaceFirst(regex.toRegex(), replacement)"))
//public fun String.replaceFirst(regex: String, replacement: String): String = (this as java.lang.String).replaceFirst(regex, replacement)

/**
 * Returns the character (Unicode code point) at the specified index.
 */
public fun String.codePointAt(index: Int): Int = (this as java.lang.String).codePointAt(index)

/**
 * Returns the character (Unicode code point) before the specified index.
 */
public fun String.codePointBefore(index: Int): Int = (this as java.lang.String).codePointBefore(index)

/**
 * Returns the number of Unicode code points in the specified text range of this String.
 */
public fun String.codePointCount(beginIndex: Int, endIndex: Int): Int = (this as java.lang.String).codePointCount(beginIndex, endIndex)

/**
 * Compares two strings lexicographically, ignoring case differences.
 */
@Deprecated("Use compareTo with true passed to ignoreCase parameter.", ReplaceWith("compareTo(str, ignoreCase = true)"))
public fun String.compareToIgnoreCase(str: String): Int = (this as java.lang.String).compareToIgnoreCase(str)

/**
 * Compares two strings lexicographically, optionally ignoring case differences.
 */
public fun String.compareTo(other: String, ignoreCase: Boolean = false): Int {
    if (ignoreCase)
        return (this as java.lang.String).compareToIgnoreCase(other)
    else
        return (this as java.lang.String).compareTo(other)
}

/**
 * Returns a new string obtained by concatenating this string and the specified string.
 */
public fun String.concat(str: String): String = (this as java.lang.String).concat(str)

/**
 * Returns `true` if this string is equal to the contents of the specified CharSequence.
 */
public fun String.contentEquals(cs: CharSequence): Boolean = (this as java.lang.String).contentEquals(cs)

/**
 * Returns `true` if this string is equal to the contents of the specified StringBuffer.
 */
public fun String.contentEquals(sb: StringBuffer): Boolean = (this as java.lang.String).contentEquals(sb)

/**
 * Copies the characters from a substring of this string into the specified character array.
 * @param srcBegin the start offset (inclusive) of the substring to copy.
 * @param srcEnd the end offset (exclusive) of the substring to copy.
 * @param dst the array to copy to.
 * @param dstBegin the position in the array to copy to.
 */
public fun String.getChars(srcBegin: Int, srcEnd: Int, dst: CharArray, dstBegin: Int): Unit = (this as java.lang.String).getChars(srcBegin, srcEnd, dst, dstBegin)


/**
 * Returns a canonical representation for this string object.
 */
public fun String.intern(): String = (this as java.lang.String).intern()

/**
 * Returns `true` if this string is empty or consists solely of whitespace characters.
 */
public fun String.isBlank(): Boolean = length() == 0 || all { it.isWhitespace() }

/**
 * Returns `true` if this string matches the given regular expression.
 */
@Deprecated("Use String.matches(Regex) instead. You can convert regex parameter with .toRegex() extension function.", ReplaceWith("matches(regex.toRegex())"))
public fun String.matches(regex: String): Boolean = (this as java.lang.String).matches(regex)

/**
 * Returns the index within this string that is offset from the given [index] by [codePointOffset] code points.
 */
public fun String.offsetByCodePoints(index: Int, codePointOffset: Int): Int = (this as java.lang.String).offsetByCodePoints(index, codePointOffset)

/**
 * Returns `true` if the specified range in this string is equal to the specified range in another string.
 * @param ignoreCase if `true`, character case is ignored when comparing.
 * @param toffset the start offset in this string of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param ooffset the start offset in the other string of the substring to compare.
 * @param len the length of the substring to compare.
 */
@Deprecated("Use regionMatches overload with ignoreCase as optional last parameter.", ReplaceWith("regionMatches(toffset, other, ooffset, len, ignoreCase)"))
public fun String.regionMatches(ignoreCase: Boolean, toffset: Int, other: String, ooffset: Int, len: Int): Boolean = (this as java.lang.String).regionMatches(ignoreCase, toffset, other, ooffset, len)

/**
 * Returns `true` if the specified range in this string is equal to the specified range in another string.
 * @param thisOffset the start offset in this string of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other string of the substring to compare.
 * @param length the length of the substring to compare.
 */
public fun String.regionMatches(thisOffset: Int, other: String, otherOffset: Int, length: Int, ignoreCase: Boolean = false): Boolean =
        if (!ignoreCase)
            (this as java.lang.String).regionMatches(thisOffset, other, otherOffset, length)
        else
            (this as java.lang.String).regionMatches(ignoreCase, thisOffset, other, otherOffset, length)

/**
 * Returns a copy of this string converted to lower case using the rules of the specified locale.
 */
public fun String.toLowerCase(locale: java.util.Locale): String = (this as java.lang.String).toLowerCase(locale)

/**
 * Returns a copy of this string converted to upper case using the rules of the specified locale.
 */
public fun String.toUpperCase(locale: java.util.Locale): String = (this as java.lang.String).toUpperCase(locale)

/**
 * Returns `true` if the contents of this string is equal to the word "true", ignoring case, and `false` otherwise.
 */
public fun String.toBoolean(): Boolean = java.lang.Boolean.parseBoolean(this)

/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public fun String.toShort(): Short = java.lang.Short.parseShort(this)

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public fun String.toInt(): Int = java.lang.Integer.parseInt(this)

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public fun String.toLong(): Long = java.lang.Long.parseLong(this)

/**
 * Parses the string as a [Float] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public fun String.toFloat(): Float = java.lang.Float.parseFloat(this)

/**
 * Parses the string as a [Double] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public fun String.toDouble(): Double = java.lang.Double.parseDouble(this)

/**
 * Returns the list of all characters in this string.
 */
public fun String.toCharList(): List<Char> = toCharArray().toList()

/**
 * Returns a subsequence of this sequence.
 *
 * @param start the start index (inclusive).
 * @param end the end index (exclusive).
 */
public operator fun CharSequence.get(start: Int, end: Int): CharSequence = subSequence(start, end)

/**
 * Encodes the contents of this string using the specified character set and returns the resulting byte array.
 */
public fun String.toByteArray(charset: String): ByteArray = (this as java.lang.String).getBytes(charset)

/**
 * Encodes the contents of this string using the specified character set and returns the resulting byte array.
 */
public fun String.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray = (this as java.lang.String).getBytes(charset)

@Deprecated("Use toByteArray() instead to emphasize copy behaviour", ReplaceWith("toByteArray()"))
public fun String.getBytes(): ByteArray = (this as java.lang.String).getBytes()

@Deprecated("Use toByteArray(charset) instead to emphasize copy behaviour", ReplaceWith("toByteArray(charset)"))
public fun String.getBytes(charset: Charset): ByteArray = (this as java.lang.String).getBytes(charset)

@Deprecated("Use toByteArray(charset) instead to emphasize copy behaviour", ReplaceWith("toByteArray(charset)"))
public fun String.getBytes(charset: String): ByteArray = (this as java.lang.String).getBytes(charset)

/**
 * Returns a subsequence of this sequence specified by given [range].
 */
public fun CharSequence.slice(range: IntRange): CharSequence {
    return subSequence(range.start, range.end + 1) // inclusive
}

/**
 * Converts the string into a regular expression [Pattern] optionally
 * with the specified [flags] from [Pattern] or'd together
 * so that strings can be split or matched on.
 */
public fun String.toPattern(flags: Int = 0): java.util.regex.Pattern {
    return java.util.regex.Pattern.compile(this, flags)
}

/**
 * Creates a new [StringReader] for reading the contents of this string.
 */
@Deprecated("Use reader() method instead in kotlin.io package", ReplaceWith("this.reader()", "kotlin.io.reader"))
public val String.reader: StringReader
    get() = StringReader(this)

/**
 * Returns a copy of this string having its first letter uppercased, or the original string,
 * if it's empty or already starts with an upper case letter.
 *
 * @sample test.text.StringTest.capitalize
 */
public fun String.capitalize(): String {
    return if (isNotEmpty() && charAt(0).isLowerCase()) substring(0, 1).toUpperCase() + substring(1) else this
}

/**
 * Returns a copy of this string having its first letter lowercased, or the original string,
 * if it's empty or already starts with a lower case letter.
 *
 * @sample test.text.StringTest.decapitalize
 */
public fun String.decapitalize(): String {
    return if (isNotEmpty() && charAt(0).isUpperCase()) substring(0, 1).toLowerCase() + substring(1) else this
}

/**
 * Repeats a given string [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 * @sample test.text.StringJVMTest.repeat
 */
public fun String.repeat(n: Int): String {
    if (n < 0)
        throw IllegalArgumentException("Value should be non-negative, but was $n")

    val sb = StringBuilder()
    for (i in 1..n) {
        sb.append(this)
    }
    return sb.toString()
}

/**
 * Appends the contents of this string, excluding the first characters that satisfy the given [predicate],
 * to the given Appendable.
 */
public inline fun <T : Appendable> String.dropWhileTo(result: T, predicate: (Char) -> Boolean): T {
    var start = true
    for (element in this) {
        if (start && predicate(element)) {
            // ignore
        } else {
            start = false
            result.append(element)
        }
    }
    return result
}

/**
 * Appends the first characters from this string that satisfy the given [predicate] to the given Appendable.
 */
public inline fun <T : Appendable> String.takeWhileTo(result: T, predicate: (Char) -> Boolean): T {
    for (c in this) if (predicate(c)) result.append(c) else break
    return result
}

/**
 * Replaces every [regexp] occurence in the text with the value returned by the given function [body] that
 * takes a [MatchResult].
 */
@Deprecated("Use String.replace(Regex, (MatchResult)->String) instead.  You can convert regex parameter with .toRegex() extension function.")
public fun String.replaceAll(regexp: String, body: (java.util.regex.MatchResult) -> String): String {
    val sb = StringBuilder(this.length())
    val p = regexp.toPattern()
    val m = p.matcher(this)

    var lastIdx = 0
    while (m.find()) {
        sb.append(this, lastIdx, m.start())
        sb.append(body(m.toMatchResult()))
        lastIdx = m.end()
    }

    if (lastIdx == 0) {
        return this;
    }

    sb.append(this, lastIdx, this.length())

    return sb.toString()
}

/**
 * A Comparator that orders strings ignoring character case.
 *
 * Note that this Comparator does not take locale into account,
 * and will result in an unsatisfactory ordering for certain locales.
 */
public val String.Companion.CASE_INSENSITIVE_ORDER: Comparator<String>
    get() = java.lang.String.CASE_INSENSITIVE_ORDER
