package kotlin

import java.io.StringReader
import java.util.ArrayList
import java.util.Locale
import java.util.regex.MatchResult
import java.util.regex.Pattern
import java.nio.charset.Charset

public fun String.lastIndexOf(str: String): Int = (this as java.lang.String).lastIndexOf(str)

public fun String.lastIndexOf(ch: Char): Int = (this as java.lang.String).lastIndexOf(ch.toString())

public fun String.equalsIgnoreCase(anotherString: String): Boolean = (this as java.lang.String).equalsIgnoreCase(anotherString)

public fun String.hashCode(): Int = (this as java.lang.String).hashCode()

public fun String.indexOf(str: String): Int = (this as java.lang.String).indexOf(str)

public fun String.indexOf(str: String, fromIndex: Int): Int = (this as java.lang.String).indexOf(str, fromIndex)

public fun String.replace(oldChar: Char, newChar: Char): String = (this as java.lang.String).replace(oldChar, newChar)

public fun String.replaceAll(regex: String, replacement: String): String = (this as java.lang.String).replaceAll(regex, replacement)

public fun String.trim(): String = (this as java.lang.String).trim()

public fun String.toUpperCase(): String = (this as java.lang.String).toUpperCase()

public fun String.toLowerCase(): String = (this as java.lang.String).toLowerCase()

public fun String.toCharArray(): CharArray = (this as java.lang.String).toCharArray()

public fun String.format(vararg args: Any?): String = java.lang.String.format(this, *args)

public fun String.format(locale: Locale, vararg args : Any?) : String = java.lang.String.format(locale, this, *args)

public fun String.split(regex: String): Array<String> = (this as java.lang.String).split(regex)

public fun String.split(ch: Char): Array<String> = (this as java.lang.String).split(java.util.regex.Pattern.quote(ch.toString()))

public fun String.substring(beginIndex: Int): String = (this as java.lang.String).substring(beginIndex)

public fun String.substring(beginIndex: Int, endIndex: Int): String = (this as java.lang.String).substring(beginIndex, endIndex)

public fun String.startsWith(prefix: String): Boolean = (this as java.lang.String).startsWith(prefix)

public fun String.startsWith(prefix: String, toffset: Int): Boolean = (this as java.lang.String).startsWith(prefix, toffset)

public fun String.startsWith(ch: Char): Boolean = (this as java.lang.String).startsWith(ch.toString())

public fun String.contains(seq: CharSequence): Boolean = (this as java.lang.String).contains(seq)

public fun String.endsWith(suffix: String): Boolean = (this as java.lang.String).endsWith(suffix)

public fun String.endsWith(ch: Char): Boolean = (this as java.lang.String).endsWith(ch.toString())

// "constructors" for String

public fun String(bytes: ByteArray, offset: Int, length: Int, charsetName: String): String = java.lang.String(bytes, offset, length, charsetName) as String

public fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String = java.lang.String(bytes, offset, length, charset) as String

public fun String(bytes: ByteArray, charsetName: String): String = java.lang.String(bytes, charsetName) as String

public fun String(bytes: ByteArray, charset: Charset): String = java.lang.String(bytes, charset) as String

public fun String(bytes: ByteArray, i: Int, i1: Int): String = java.lang.String(bytes, i, i1, Charsets.UTF_8) as String

public fun String(bytes: ByteArray): String = java.lang.String(bytes, Charsets.UTF_8) as String

public fun String(chars: CharArray): String = java.lang.String(chars) as String

public fun String(stringBuffer: java.lang.StringBuffer): String = java.lang.String(stringBuffer) as String

public fun String(stringBuilder: java.lang.StringBuilder): String = java.lang.String(stringBuilder) as String

public fun String.replaceFirst(regex: String, replacement: String): String = (this as java.lang.String).replaceFirst(regex, replacement)

public fun String.split(regex: String, limit: Int): Array<String> = (this as java.lang.String).split(regex, limit)

public fun String.codePointAt(index: Int): Int = (this as java.lang.String).codePointAt(index)

public fun String.codePointBefore(index: Int): Int = (this as java.lang.String).codePointBefore(index)

public fun String.codePointCount(beginIndex: Int, endIndex: Int): Int = (this as java.lang.String).codePointCount(beginIndex, endIndex)

public fun String.compareToIgnoreCase(str: String): Int = (this as java.lang.String).compareToIgnoreCase(str)

public fun String.concat(str: String): String = (this as java.lang.String).concat(str)

public fun String.contentEquals(cs: CharSequence): Boolean = (this as java.lang.String).contentEquals(cs)

public fun String.contentEquals(sb: StringBuffer): Boolean = (this as java.lang.String).contentEquals(sb)

public fun String.getChars(srcBegin: Int, srcEnd: Int, dst: CharArray, dstBegin: Int): Unit = (this as java.lang.String).getChars(srcBegin, srcEnd, dst, dstBegin)

public fun String.indexOf(ch: Char): Int = (this as java.lang.String).indexOf(ch.toString())

public fun String.indexOf(ch: Char, fromIndex: Int): Int = (this as java.lang.String).indexOf(ch.toString(), fromIndex)

public fun String.intern(): String = (this as java.lang.String).intern()

public fun String.isEmpty(): Boolean = (this as java.lang.String).isEmpty()

public fun String.lastIndexOf(ch: Char, fromIndex: Int): Int = (this as java.lang.String).lastIndexOf(ch.toString(), fromIndex)

public fun String.lastIndexOf(str: String, fromIndex: Int): Int = (this as java.lang.String).lastIndexOf(str, fromIndex)

public fun String.matches(regex: String): Boolean = (this as java.lang.String).matches(regex)

public fun String.offsetByCodePoints(index: Int, codePointOffset: Int): Int = (this as java.lang.String).offsetByCodePoints(index, codePointOffset)

public fun String.regionMatches(ignoreCase: Boolean, toffset: Int, other: String, ooffset: Int, len: Int): Boolean = (this as java.lang.String).regionMatches(ignoreCase, toffset, other, ooffset, len)

public fun String.regionMatches(toffset: Int, other: String, ooffset: Int, len: Int): Boolean = (this as java.lang.String).regionMatches(toffset, other, ooffset, len)

public fun String.replace(target: CharSequence, replacement: CharSequence): String = (this as java.lang.String).replace(target, replacement)

public fun String.toLowerCase(locale: java.util.Locale): String = (this as java.lang.String).toLowerCase(locale)

public fun String.toUpperCase(locale: java.util.Locale): String = (this as java.lang.String).toUpperCase(locale)

public fun String.toBoolean(): Boolean = java.lang.Boolean.parseBoolean(this)
public fun String.toShort(): Short = java.lang.Short.parseShort(this)
public fun String.toInt(): Int = java.lang.Integer.parseInt(this)
public fun String.toLong(): Long = java.lang.Long.parseLong(this)
public fun String.toFloat(): Float = java.lang.Float.parseFloat(this)
public fun String.toDouble(): Double = java.lang.Double.parseDouble(this)

public fun String.toCharList(): List<Char> = toCharArray().toList()

/**
 * Returns a subsequence of this sequence.
 *
 * @param start the start index (inclusive).
 * @param end the end index (exclusive).
 */
public fun CharSequence.get(start: Int, end: Int): CharSequence = subSequence(start, end)

public fun String.toByteArray(charset: String): ByteArray = (this as java.lang.String).getBytes(charset)
public fun String.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray = (this as java.lang.String).getBytes(charset)

deprecated("Use toByteArray() instead to emphasize copy behaviour")
public fun String.getBytes(): ByteArray = (this as java.lang.String).getBytes()

deprecated("Use toByteArray(charset) instead to emphasize copy behaviour")
public fun String.getBytes(charset: Charset): ByteArray = (this as java.lang.String).getBytes(charset)

deprecated("Use toByteArray(charset) instead to emphasize copy behaviour")
public fun String.getBytes(charset: String): ByteArray = (this as java.lang.String).getBytes(charset)

/**
 * Returns a subsequence specified by given [range].
 */
public fun CharSequence.slice(range: IntRange): CharSequence {
    return subSequence(range.start, range.end + 1) // inclusive
}

/**
 * Converts the string into a regular expression [Pattern] optionally
 * with the specified flags from [Pattern] or'd together
 * so that strings can be split or matched on.
 */
public fun String.toRegex(flags: Int = 0): java.util.regex.Pattern {
    return java.util.regex.Pattern.compile(this, flags)
}

/**
 * Creates a new [StringReader] for reading the contents of this string.
 */
public val String.reader: StringReader
    get() = StringReader(this)

/**
 * Returns a copy of this string capitalised if it is not empty or already starting with an upper case letter,
 * otherwise returns this.
 *
 * @sample test.text.StringTest.capitalize
 */
public fun String.capitalize(): String {
    return if (isNotEmpty() && charAt(0).isLowerCase()) substring(0, 1).toUpperCase() + substring(1) else this
}

/**
 * Returns a copy of this string with the first letter lowercased if it is not empty or already starting with
 * a lower case letter, otherwise returns this.
 *
 * @sample test.text.StringTest.decapitalize
 */
public fun String.decapitalize(): String {
    return if (isNotEmpty() && charAt(0).isUpperCase()) substring(0, 1).toLowerCase() + substring(1) else this
}

/**
 * Repeats a given string [n] times.
 * @throws IllegalArgumentException when n < 0
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
public fun String.replaceAll(regexp: String, body: (java.util.regex.MatchResult) -> String): String {
    val sb = StringBuilder(this.length())
    val p = regexp.toRegex()
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

