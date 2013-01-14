package kotlin

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList

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
public inline fun String.filter(predicate: (Char) -> Boolean): String = filterTo(StringBuilder(), predicate).toString()

/**
 * Returns an Appendable containing all characters which match the given *predicate*
 *
 * @includeFunctionBody ../../test/StringTest.kt filter
 */
public inline fun <T: Appendable> String.filterTo(result: T, predicate: (Char) -> Boolean): T
{
    for (с in this) if (predicate(с)) result.append(с)
    return result
}

/**
 * Filters characters which match the given predicate into new String object
 *
 * @includeFunctionBody ../../test/StringTest.kt filterNot
 */
public inline fun String.filterNot(predicate: (Char) -> Boolean): String = filterNotTo(StringBuilder(), predicate).toString()

/**
 * Returns an Appendable containing all characters which do not match the given *predicate*
 *
 * @includeFunctionBody ../../test/StringTest.kt filterNot
 */
public inline fun <T: Appendable> String.filterNotTo(result: T, predicate: (Char) -> Boolean): T {
    for (element in this) if (!predicate(element)) result.append(element)
    return result
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

/**
 * Appends the string from all the characters separated using the *separator* and using the given *prefix* and *postfix* if supplied
 *
 * If a string could be huge you can specify a non-negative value of *limit* which will only show substring then it will
 * a special *truncated* separator (which defaults to "..."
 *
 * @includeFunctionBody ../../test/StringTest.kt appendString
 */
public inline fun String.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
    buffer.append(prefix)
    var count = 0
    for (c in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) buffer.append(c) else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
}

/**
 * Returns the first character which matches the given *predicate* or *null* if none matched
 *
 * @includeFunctionBody ../../test/StringTest.kt find
 */
public inline fun String.find(predicate: (Char) -> Boolean): Char? {
    for (c in this) if (predicate(c)) return c
    return null
}

/**
 * Returns the first character which does not match the given *predicate* or *null* if none matched
 *
 * @includeFunctionBody ../../test/StringTest.kt findNot
 */
public inline fun String.findNot(predicate: (Char) -> Boolean): Char? {
    for (c in this) if (!predicate(c)) return c
    return null
}

/**
* Partitions this string into a pair of string
*
* @includeFunctionBody ../../test/StringTest.kt partition
*/
public inline fun String.partition(predicate: (Char) -> Boolean): Pair<String, String> {
    val first = StringBuilder()
    val second = StringBuilder()
    for (c in this) {
        if (predicate(c)) {
            first.append(c)
        } else {
            second.append(c)
        }
    }
    return Pair(first.toString(), second.toString())
}

/**
 * Returns the result of transforming each character to one or more values which are concatenated together into a single list
 *
 * @includeFunctionBody ../../test/StringTest.kt flatMap
 */
public inline fun <R> String.flatMap(transform: (Char) -> Collection<R>): Collection<R> = flatMapTo(ArrayList<R>(), transform)

/**
 * Returns the result of transforming each character to one or more values which are concatenated together into a passed list
 *
 * @includeFunctionBody ../../test/StringTest.kt flatMap
 */
public inline fun <R> String.flatMapTo(result: MutableCollection<R>, transform: (Char) -> Collection<R>): Collection<R> {
    for (c in this) result.addAll(transform(c))
    return result
}

/**
 * Folds all characters from left to right with the *initial* value to perform the operation on sequential pairs of characters
 *
 * @includeFunctionBody ../../test/StringTest.kt fold
 */
public inline fun <R> String.fold(initial: R, operation: (R, Char) -> R): R {
    var answer = initial
    for (c in this) answer = operation(answer, c)
    return answer
}

/**
 * Folds all characters from right to left with the *initial* value to perform the operation on sequential pairs of characters
 *
 * @includeFunctionBody ../../test/StringTest.kt foldRight
 */
public inline fun <R> String.foldRight(initial: R, operation: (Char, R) -> R): R = reverse().fold(initial, { x, y -> operation(y, x) })

/**
 * Applies binary operation to all characters in a string, going from left to right.
 * Similar to fold function, but uses the first character as initial value
 *
 * @includeFunctionBody ../../test/StringTest.kt reduce
 */
public inline fun String.reduce(operation: (Char, Char) -> Char): Char {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw UnsupportedOperationException("Empty string can't be reduced")
    }

    var result = iterator.next()
    while (iterator.hasNext()) {
        result = operation(result, iterator.next())
    }

    return result
}

/**
 * Applies binary operation to all characters in a string, going from right to left.
 * Similar to foldRight function, but uses the last character as initial value
 *
 * @includeFunctionBody ../../test/StringTest.kt reduceRight
 */
public inline fun String.reduceRight(operation: (Char, Char) -> Char): Char = reverse().reduce { x, y -> operation(y, x) }


/**
 * Groups the characters in the string into a new [[Map]] using the supplied *toKey* function to calculate the key to group the characters by
 *
 * @includeFunctionBody ../../test/StringTest.kt groupBy
 */
public inline fun <K> String.groupBy(toKey: (Char) -> K): Map<K, String> = groupByTo<K>(HashMap<K, String>(), toKey)

/**
 * Groups the characters in the string into the given [[Map]] using the supplied *toKey* function to calculate the key to group the characters by
 *
 * @includeFunctionBody ../../test/StringTest.kt groupBy
 */
public inline fun <K> String.groupByTo(result: MutableMap<K, String>, toKey: (Char) -> K): Map<K, String> {
    for (c in this) {
        val key = toKey(c)
        val str = result.getOrElse(key) { "" }
        result[key] = str + c
    }
    return result
}

/**
 * Creates a new string from all the characters separated using the *separator* and using the given *prefix* and *postfix* if supplied.
 *
 * If a string could be huge you can specify a non-negative value of *limit* which will only show a substring then it will
 * a special *truncated* separator (which defaults to "..."
 *
 * @includeFunctionBody ../../test/StringTest.kt makeString
 */
public inline fun String.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    val buffer = StringBuilder()
    appendString(buffer, separator, prefix, postfix, limit, truncated)
    return buffer.toString()
}

/**
 * Returns an Appendable containing the everything but the first characters that satisfy the given *predicate*
 *
 * @includeFunctionBody ../../test/StringTest.kt dropWhile
 */
public inline fun <T: Appendable> String.dropWhileTo(result: T, predicate: (Char) -> Boolean): T {
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
 * Returns a new String containing the everything but the first characters that satisfy the given *predicate*
 *
 * @includeFunctionBody ../../test/StringTest.kt dropWhile
 */
public inline fun String.dropWhile(predicate: (Char) -> Boolean): String = dropWhileTo(StringBuilder(), predicate).toString()

/**
 * Returns a string containing everything but the first *n* characters
 *
 * @includeFunctionBody ../../test/StringTest.kt drop
 */
public inline fun String.drop(n: Int): String = dropWhile(countTo(n))

/**
 * Returns an Appendable containing the first characters that satisfy the given *predicate*
  *
 * @includeFunctionBody ../../test/StringTest.kt takeWhile
 */
public inline fun <T: Appendable> String.takeWhileTo(result: T, predicate: (Char) -> Boolean): T {
    for (c in this) if (predicate(c)) result.append(c) else break
    return result
}

/**
 * Returns a new String containing the first characters that satisfy the given *predicate*
  *
 * @includeFunctionBody ../../test/StringTest.kt takeWhile
 */
public inline fun String.takeWhile(predicate: (Char) -> Boolean): String = takeWhileTo(StringBuilder(), predicate).toString()

/**
 * Returns a string containing the first *n* characters
 *
 * @includeFunctionBody ../../test/StringTest.kt take
 */
public inline fun String.take(n: Int): String = takeWhile(countTo(n))

/** Copies all characters into the given collection */
public inline fun <C: MutableCollection<in Char>> String.toCollection(result: C): C {
    for (c in this) result.add(c)
    return result
}

/** Copies all characters into a [[LinkedList]]  */
public inline fun String.toLinkedList(): LinkedList<Char> = toCollection(LinkedList<Char>())

/** Copies all characters into a [[List]] */
public inline fun String.toList(): List<Char> = toCollection(ArrayList<Char>(this.length()))

/** Copies all characters into a [[Collection] */
public inline fun String.toCollection(): Collection<Char> = toCollection(ArrayList<Char>(this.length()))

/** Copies all characters into a [[Set]] */
public inline fun String.toSet(): Set<Char> = toCollection(HashSet<Char>())
