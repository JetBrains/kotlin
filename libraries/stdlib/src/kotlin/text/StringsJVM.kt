package kotlin

import java.io.StringReader
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList
import java.util.Locale
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

public fun String.length(): Int = (this as java.lang.String).length()

public fun String.getBytes(): ByteArray = (this as java.lang.String).getBytes()

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

public fun String(bytes: ByteArray, i: Int, i1: Int): String = java.lang.String(bytes, i, i1) as String

public fun String(bytes: ByteArray): String = java.lang.String(bytes) as String

public fun String(chars: CharArray): String = java.lang.String(chars) as String

public fun String(stringBuffer: java.lang.StringBuffer): String = java.lang.String(stringBuffer) as String

public fun String(stringBuilder: java.lang.StringBuilder): String = java.lang.String(stringBuilder) as String

public fun String.replaceFirst(regex: String, replacement: String): String = (this as java.lang.String).replaceFirst(regex, replacement)

public fun String.charAt(index: Int): Char = (this as java.lang.String).charAt(index)

public fun String.split(regex: String, limit: Int): Array<String> = (this as java.lang.String).split(regex, limit)

public fun String.codePointAt(index: Int): Int = (this as java.lang.String).codePointAt(index)

public fun String.codePointBefore(index: Int): Int = (this as java.lang.String).codePointBefore(index)

public fun String.codePointCount(beginIndex: Int, endIndex: Int): Int = (this as java.lang.String).codePointCount(beginIndex, endIndex)

public fun String.compareToIgnoreCase(str: String): Int = (this as java.lang.String).compareToIgnoreCase(str)

public fun String.concat(str: String): String = (this as java.lang.String).concat(str)

public fun String.contentEquals(cs: CharSequence): Boolean = (this as java.lang.String).contentEquals(cs)

public fun String.contentEquals(sb: StringBuffer): Boolean = (this as java.lang.String).contentEquals(sb)

public fun String.getBytes(charset: Charset): ByteArray = (this as java.lang.String).getBytes(charset)

public fun String.getBytes(charsetName: String): ByteArray = (this as java.lang.String).getBytes(charsetName)

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

public fun String.subSequence(beginIndex: Int, endIndex: Int): CharSequence = (this as java.lang.String).subSequence(beginIndex, endIndex)

public fun String.toLowerCase(locale: java.util.Locale): String = (this as java.lang.String).toLowerCase(locale)

public fun String.toUpperCase(locale: java.util.Locale): String = (this as java.lang.String).toUpperCase(locale)

public fun CharSequence.charAt(index: Int): Char = (this as java.lang.CharSequence).charAt(index)

public fun CharSequence.subSequence(start: Int, end: Int): CharSequence? = (this as java.lang.CharSequence).subSequence(start, end)

public fun CharSequence.toString(): String? = (this as java.lang.CharSequence).toString()

public fun CharSequence.length(): Int = (this as java.lang.CharSequence).length()

public fun String.toByteArray(encoding: Charset): ByteArray = (this as java.lang.String).getBytes(encoding)

public fun String.toBoolean(): Boolean = java.lang.Boolean.parseBoolean(this)
public fun String.toShort(): Short = java.lang.Short.parseShort(this)
public fun String.toInt(): Int = java.lang.Integer.parseInt(this)
public fun String.toLong(): Long = java.lang.Long.parseLong(this)
public fun String.toFloat(): Float = java.lang.Float.parseFloat(this)
public fun String.toDouble(): Double = java.lang.Double.parseDouble(this)

public fun String.toCharList(): List<Char> = toCharArray().toList()

public fun CharSequence.get(index: Int): Char = charAt(index)
public fun CharSequence.get(start: Int, end: Int): CharSequence? = subSequence(start, end)
public fun String.toByteArray(encoding: String = Charset.defaultCharset().name()): ByteArray = (this as java.lang.String).getBytes(encoding)

/**
 * Returns a subsequence specified by given range.
 */
public fun CharSequence.slice(range: IntRange): CharSequence {
    return subSequence(range.start, range.end + 1)!! // inclusive
}


/**
 * Converts the string into a regular expression [[Pattern]] optionally
 * with the specified flags from [[Pattern]] or'd together
 * so that strings can be split or matched on.
 */
public fun String.toRegex(flags: Int = 0): java.util.regex.Pattern {
    return java.util.regex.Pattern.compile(this, flags)
}

public val String.reader: StringReader
    get() = StringReader(this)

/**
 * Returns a copy of this string capitalised if it is not empty or already starting with an uppper case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/text/StringTest.kt capitalize
 */
public fun String.capitalize(): String {
    return if (isNotEmpty() && charAt(0).isLowerCase()) substring(0, 1).toUpperCase() + substring(1) else this
}

/**
 * Returns a copy of this string with the first letter lower case if it is not empty or already starting with a lower case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/text/StringTest.kt decapitalize
 */
public fun String.decapitalize(): String {
    return if (isNotEmpty() && charAt(0).isUpperCase()) substring(0, 1).toLowerCase() + substring(1) else this
}

/**
 * Repeats a given string n times.
 * When n < 0, IllegalArgumentException is thrown.
 * @includeFunctionBody ../../test/text/StringTest.kt repeat
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
 * Returns the first character which matches the given *predicate* or *null* if none matched
 *
 * @includeFunctionBody ../../test/text/StringTest.kt find
 */
deprecated("Use firstOrNull instead")
public inline fun String.find(predicate: (Char) -> Boolean): Char? {
    for (c in this) if (predicate(c)) return c
    return null
}

/**
 * Returns the first character which does not match the given *predicate* or *null* if none matched
 *
 * @includeFunctionBody ../../test/text/StringTest.kt findNot
 */
deprecated("Use firstOrNull instead")
public inline fun String.findNot(predicate: (Char) -> Boolean): Char? {
    for (c in this) if (!predicate(c)) return c
    return null
}

/**
 * Returns an Appendable containing the everything but the first characters that satisfy the given *predicate*
 *
 * @includeFunctionBody ../../test/text/StringTest.kt dropWhile
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
 * Returns an Appendable containing the first characters that satisfy the given *predicate*
 *
 * @includeFunctionBody ../../test/text/StringTest.kt takeWhile
 */
public inline fun <T : Appendable> String.takeWhileTo(result: T, predicate: (Char) -> Boolean): T {
    for (c in this) if (predicate(c)) result.append(c) else break
    return result
}

/** Copies all characters into a [[Collection] */
deprecated("Use toList() instead.")
public fun String.toCollection(): Collection<Char> = toCollection(ArrayList<Char>(this.length()))

/** Returns a new String containing the everything but the leading whitespace characters */
public fun String.trimLeading(): String {
    var count = 0

    while ((count < this.length) && (this[count] <= ' ')) {
        count++
    }
    return if (count > 0) substring(count) else this
}

/** Returns a new String containing the everything but the trailing whitespace characters */
public fun String.trimTrailing(): String {
    var count = this.length

    while (count > 0 && this[count - 1] <= ' ') {
        count--
    }
    return if (count < this.length) substring(0, count) else this
}

/**
 * Replaces every *regexp* occurence in the text with the value retruned by the given function *body* that can handle
 * particular occurance using [[MatchResult]] provided.
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

