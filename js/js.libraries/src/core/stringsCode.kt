package js

public inline fun String.lastIndexOf(ch : Char, fromIndex : Int) : Int = lastIndexOf(ch.toString(), fromIndex)
public inline fun String.lastIndexOf(ch: Char) : Int = lastIndexOf(ch.toString())

public inline fun String.indexOf(ch : Char) : Int = indexOf(ch.toString())
public inline fun String.indexOf(ch : Char, fromIndex : Int) : Int = indexOf(ch.toString(), fromIndex)

public inline fun String.matches(regex : String) : Boolean {
    val result = this.match(regex)
    return result != null && result.size > 0
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
