package kotlin.text

import kotlin.js.RegExp

internal inline fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int = nativeIndexOf(ch.toString(), fromIndex)
internal inline fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int = nativeLastIndexOf(ch.toString(), fromIndex)

/**
 * Returns `true` if this string starts with the specified prefix.
 */
public fun String.startsWith(prefix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return nativeStartsWith(prefix, 0)
    else
        return regionMatches(0, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if a substring of this string starting at the specified offset [startIndex] starts with the specified prefix.
 */
public fun String.startsWith(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return nativeStartsWith(prefix, startIndex)
    else
        return regionMatches(startIndex, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if this string ends with the specified suffix.
 */
public fun String.endsWith(suffix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return nativeEndsWith(suffix)
    else
        return regionMatches(length - suffix.length, suffix, 0, suffix.length, ignoreCase)
}



public inline fun String.matches(regex: String): Boolean {
    val result = this.match(regex)
    return result != null && result.size > 0
}

public fun CharSequence.isBlank(): Boolean = length == 0 || (if (this is String) this else this.toString()).matches("^[\\s\\xA0]+$")

public fun String?.equals(other: String?, ignoreCase: Boolean = false): Boolean =
        if (this == null)
            other == null
        else if (!ignoreCase)
            this == other
        else
            other != null && this.toLowerCase() == other.toLowerCase()


public fun CharSequence.regionMatches(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean = false): Boolean
        = regionMatchesImpl(thisOffset, other, otherOffset, length, ignoreCase)


/**
 * Returns a copy of this string capitalised if it is not empty or already starting with an uppper case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/StringTest.kt capitalize
 */
public inline fun String.capitalize(): String {
    return if (isNotEmpty()) substring(0, 1).toUpperCase() + substring(1) else this
}

/**
 * Returns a copy of this string with the first letter lower case if it is not empty or already starting with a lower case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/StringTest.kt decapitalize
 */
public inline fun String.decapitalize(): String {
    return if (isNotEmpty()) substring(0, 1).toLowerCase() + substring(1) else this
}

/**
 * Returns a string containing this char sequence repeated [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 */
public fun CharSequence.repeat(n: Int): String {
    require(n >= 0) { "Count 'n' must be non-negative, but was $n." }
    return when (n) {
        0 -> ""
        1 -> this.toString()
        else -> {
            var result = ""
            if (!isEmpty()) {
                var s = this.toString()
                var count = n
                while (true) {
                    if ((count and 1) == 1) {
                        result += s
                    }
                    count = count ushr 1
                    if (count == 0) {
                        break
                    }
                    s += s
                }
            }
            return result
        }
    }
}

public fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
        nativeReplace(RegExp(Regex.escape(oldValue), if (ignoreCase) "gi" else "g"), Regex.escapeReplacement(newValue))

public fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String =
        nativeReplace(RegExp(Regex.escape(oldChar.toString()), if (ignoreCase) "gi" else "g"), newChar.toString())

public fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
        nativeReplace(RegExp(Regex.escape(oldValue), if (ignoreCase) "i" else ""), Regex.escapeReplacement(newValue))

public fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String =
        nativeReplace(RegExp(Regex.escape(oldChar.toString()), if (ignoreCase) "i" else ""), newChar.toString())
