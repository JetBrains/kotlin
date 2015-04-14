package kotlin.js

import kotlin.text.Regex
import kotlin.text.js.RegExp

// TODO: make internal
public inline fun String.nativeIndexOf(ch : Char, fromIndex : Int) : Int = nativeIndexOf(ch.toString(), fromIndex)
public inline fun String.nativeLastIndexOf(ch : Char, fromIndex : Int) : Int = nativeLastIndexOf(ch.toString(), fromIndex)

/**
 * Returns `true` if this string starts with the specified prefix.
 */
public fun String.startsWith(prefix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return nativeStartsWith(prefix, 0)
    else
        return regionMatches(0, prefix, 0, prefix.length(), ignoreCase)
}

/**
 * Returns `true` if a substring of this string starting at the specified offset [thisOffset] starts with the specified prefix.
 */
public fun String.startsWith(prefix: String, thisOffset: Int, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return nativeStartsWith(prefix, thisOffset)
    else
        return regionMatches(thisOffset, prefix, 0, prefix.length(), ignoreCase)
}

/**
 * Returns `true` if this string ends with the specified suffix.
 */
public fun String.endsWith(suffix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return nativeEndsWith(suffix)
    else
        return regionMatches(length() - suffix.length(), suffix, 0, suffix.length(), ignoreCase)
}



public inline fun String.matches(regex : String) : Boolean {
    val result = this.match(regex)
    return result != null && result.size() > 0
}


public fun String.isBlank(): Boolean = length() == 0 || matches("^[\\s\\xA0]+$")

public fun String.equals(anotherString: String, ignoreCase: Boolean = false): Boolean =
        if (!ignoreCase)
            this == anotherString
        else
            this.toLowerCase() == anotherString.toLowerCase()


public fun String.regionMatches(thisOffset: Int, other: String, otherOffset: Int, length: Int, ignoreCase: Boolean = false): Boolean {
    if ((otherOffset < 0) || (thisOffset < 0) || (thisOffset > length() - length)
        || (otherOffset > other.length() - length)) {
        return false;
    }

    return substring(thisOffset, thisOffset + length).equals(other.substring(otherOffset, otherOffset + length), ignoreCase)
}


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


public fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
        nativeReplace(RegExp(Regex.escape(oldValue), if (ignoreCase) "gi" else "g"), Regex.escapeReplacement(newValue))

public fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String =
        nativeReplace(RegExp(Regex.escape(oldChar.toString()), if (ignoreCase) "gi" else "g"), newChar.toString())

deprecated("Use replaceFirst(String, String) instead.")
public fun String.replaceFirstLiteral(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
        nativeReplace(RegExp(Regex.escape(oldValue), if (ignoreCase) "i" else ""), Regex.escapeReplacement(newValue))

public fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
        nativeReplace(RegExp(Regex.escape(oldValue), if (ignoreCase) "i" else ""), Regex.escapeReplacement(newValue))

public fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String =
        nativeReplace(RegExp(Regex.escape(oldChar.toString()), if (ignoreCase) "i" else ""), newChar.toString())
