package kotlin

import java.util.ArrayList

/** Returns the string with leading and trailing text matching the given string removed */
public inline fun String.trim(text: String) : String = trimLeading(text).trimTrailing(text)

/** Returns the string with the prefix and postfix text trimmed */
public inline fun String.trim(prefix: String, postfix: String) : String = trimLeading(prefix).trimTrailing(postfix)

/** Returns the string with the leading prefix of this string removed */
public inline fun String.trimLeading(prefix: String): String {
    var answer = this
    if (answer.startsWith(prefix)) {
        answer = answer.substring(prefix.length())
    }
    return answer
}

/** Returns the string with the trailing postfix of this string removed */
public inline fun String.trimTrailing(postfix: String): String {
    var answer = this
    if (answer.endsWith(postfix)) {
        answer = answer.substring(0, length() - postfix.length())
    }
    return answer
}

/** Returns true if the string is not null and not empty */
public inline fun String?.notEmpty() : Boolean = this != null && this.length() > 0

/**
Iterator for characters of given CharSequence
*/
public inline fun CharSequence.iterator() : CharIterator = object: jet.CharIterator() {
    private var index = 0

    public override fun nextChar(): Char = get(index++)

    public override fun hasNext(): Boolean = index < length
}

/** Returns the string if it is not null or the empty string if its null */
public inline fun String?.orEmpty(): String = this ?: ""


// "Extension functions" for CharSequence

inline val CharSequence.size : Int
get() = this.length

/**
 * Counts the number of characters which match the given predicate
 *
 * @includeFunctionBody ../../test/StringTest.kt count
 */
public inline fun String.count(predicate: (Char) -> Boolean): Int {
    var answer = 0
    for (c in this) {
        if (predicate(c)) {
            answer++
        }
    }
    return answer
}

/**
 * Filters characters which match the given predicate into new String object
 *
 * @includeFunctionBody ../../test/StringTest.kt filter
 */
public inline fun String.filter(predicate: (Char) -> Boolean): String {
    val sb = StringBuilder()
    for (c in this) {
        if(predicate(c)) {
            sb.append(c)
        }
    }
    return sb.toString()
}

/**
  * Returns order of characters into a string
  *
  * @includeFunctionBody ../../test/StringTest.kt reverse
  */
public inline fun String.reverse(): String = StringBuilder(this).reverse().toString()

/**
 * Performs the given *operation* on each character
 *
 * @includeFunctionBody ../../test/StringTest.kt forEach
 */
public inline fun String.forEach(operation: (Char) -> Unit): Unit = for(c in this) operation(c)

/**
 * Returns *true* if all characters match the given *predicate*
 *
 * @includeFunctionBody ../../test/StringTest.kt all
 */
public inline fun String.all(predicate: (Char) -> Boolean): Boolean {
    for(c in this) if(!predicate(c)) return false
    return true
}

/**
 * Returns *true* if any character matches the given *predicate*
 *
 * @includeFunctionBody ../../test/StringTest.kt any
 */
public inline fun String.any(predicate: (Char) -> Boolean): Boolean {
    for (c in this) if (predicate(c)) return true
    return false
}
