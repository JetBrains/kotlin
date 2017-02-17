package kotlin.text

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified offset.
 */
@SymbolName("Kotlin_String_indexOfChar")
external internal fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int

/**
 * Returns the index within this string of the first occurrence of the specified substring, starting from the specified offset.
 */
@SymbolName("Kotlin_String_indexOfString")
external internal fun String.nativeIndexOf(str: String, fromIndex: Int): Int

/**
 * Returns the index within this string of the last occurrence of the specified character.
 */
@SymbolName("Kotlin_String_lastIndexOfChar")
external internal fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int

/**
 * Returns the index within this string of the last occurrence of the specified character, starting from the specified offset.
 */
@SymbolName("Kotlin_String_lastIndexOfString")
external internal fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int

/**
 * Returns `true` if this string is equal to [other], optionally ignoring character case.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
public fun String?.equals(other: String?, ignoreCase: Boolean = false): Boolean {
    if (this === null)
        return other === null
    if (other === null)
        return false
    return if (!ignoreCase)
        this.equals(other)
    else
        stringEqualsIgnoreCase(this, other)
}

@SymbolName("Kotlin_String_equalsIgnoreCase")
external internal fun stringEqualsIgnoreCase(thiz: String, other: String): Boolean

/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 */
@SymbolName("Kotlin_String_replace")
external public fun String.replace(
        oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String


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
    return if (index < 0) this else this.replaceRange(index, index + oldValue.length, newValue)
}

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 */
@SymbolName("Kotlin_String_toUpperCase")
external public fun String.toUpperCase(): String

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 */
@SymbolName("Kotlin_String_toLowerCase")
external public inline fun String.toLowerCase(): String

/**
 * Returns a copy of this string having its first letter uppercased, or the original string,
 * if it's empty or already starts with an upper case letter.
 *
 * @sample samples.text.Strings.captialize
 */
public fun String.capitalize(): String {
    return if (isNotEmpty() && this[0].isLowerCase()) substring(0, 1).toUpperCase() + substring(1) else this
}

/**
 * Returns a copy of this string having its first letter lowercased, or the original string,
 * if it's empty or already starts with a lower case letter.
 *
 * @sample samples.text.Strings.decaptialize
 */
public fun String.decapitalize(): String {
    return if (isNotEmpty() && this[0].isUpperCase()) substring(0, 1).toLowerCase() + substring(1) else this
}

/**
 * Returns `true` if this string is empty or consists solely of whitespace characters.
 */
public fun CharSequence.isBlank(): Boolean = length == 0 || indices.all { this[it].isWhitespace() }

/**
 * Returns `true` if the specified range in this char sequence is equal to the specified range in another char sequence.
 * @param thisOffset the start offset in this char sequence of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other char sequence of the substring to compare.
 * @param length the length of the substring to compare.
 */
@SymbolName("Kotlin_CharSequence_regionMatches")
external public fun CharSequence.regionMatches(
        thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int,
        ignoreCase: Boolean = false): Boolean


/**
 * Returns `true` if the specified range in this string is equal to the specified range in another string.
 * @param thisOffset the start offset in this string of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other string of the substring to compare.
 * @param length the length of the substring to compare.
 */
@SymbolName("Kotlin_String_regionMatches")
external public fun String.regionMatches(
        thisOffset: Int, other: String, otherOffset: Int, length: Int,
        ignoreCase: Boolean = false): Boolean
