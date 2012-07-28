package js

public inline fun String.lastIndexOf(ch : Char, fromIndex : Int) : Int = lastIndexOf(ch.toString(), fromIndex)
public inline fun String.lastIndexOf(ch: Char) : Int = lastIndexOf(ch.toString())

public inline fun String.indexOf(ch : Char) : Int = indexOf(ch.toString())
public inline fun String.indexOf(ch : Char, fromIndex : Int) : Int = indexOf(ch.toString(), fromIndex)

public inline fun String.matches(regex : String) : Boolean {
    val result = this.match(regex)
    return result != null && result.size > 0
}

public inline fun String.length(): Int = length

inline val String.size : Int
get() = length

public inline fun String.startsWith(ch: Char): Boolean {
    return if (size > 0) charAt(0) == ch else false
}

public inline fun String.endsWith(ch: Char): Boolean {
    val s = size
    return if (s > 0) charAt(s - 1) == ch else false
}

public inline fun String.startsWith(text: String): Boolean {
    val size = text.length
    return if (size <= this.length) {
        substring(0, size) == text
    } else false
}

public inline fun String.endsWith(text: String): Boolean {
    val matchSize = text.length
    val thisSize = this.length
    return if (matchSize <= thisSize) {
        substring(thisSize - matchSize, thisSize) == text
    } else false
}


/**
 * Returns a copy of this string capitalised if it is not empty or already starting with an uppper case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/StringTest.kt capitalize
 */
public inline fun String.capitalize(): String {
    return if (notEmpty()) substring(0, 1).toUpperCase() + substring(1) else this
}

/**
 * Returns a copy of this string with the first letter lower case if it is not empty or already starting with a lower case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/StringTest.kt decapitalize
 */
public inline fun String.decapitalize(): String {
    return if (notEmpty()) substring(0, 1).toLowerCase() + substring(1) else this
}
