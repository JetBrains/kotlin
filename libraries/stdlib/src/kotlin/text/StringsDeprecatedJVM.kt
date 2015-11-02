@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

import java.io.StringReader
import java.util.regex.Pattern
import java.nio.charset.Charset
import java.util.*

/*
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

*/

/**
 * Splits this string around matches of the given regular expression.

 * @param limit Non-negative value specifying the maximum number of substrings to return.
 * Zero by default means no limit is set.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.split(regex: Pattern, limit: Int = 0): List<String>
{
    require(limit >= 0, { "Limit must be non-negative, but was $limit" } )
    return regex.split(this, if (limit == 0) -1 else limit).asList()
}

/*

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

*/
/**
 * Returns `true` if this string is empty or consists solely of whitespace characters.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.isBlank(): Boolean = length() == 0 || all { it.isWhitespace() }

/*

/**
 * Returns a subsequence of this sequence specified by given [range].
 */
public fun CharSequence.slice(range: IntRange): CharSequence {
    return subSequence(range.start, range.end + 1) // inclusive
}

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
*/

/**
 * Repeats a given string [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 * @sample test.text.StringJVMTest.repeat
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
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
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
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
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public inline fun <T : Appendable> String.takeWhileTo(result: T, predicate: (Char) -> Boolean): T {
    for (c in this) if (predicate(c)) result.append(c) else break
    return result
}

