package kotlin

import java.io.StringReader
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList
import java.nio.charset.Charset

public inline fun String.lastIndexOf(str: String) : Int = (this as java.lang.String).lastIndexOf(str)

public inline fun String.lastIndexOf(ch: Char) : Int = (this as java.lang.String).lastIndexOf(ch.toString())

public inline fun String.equalsIgnoreCase(anotherString: String) : Boolean = (this as java.lang.String).equalsIgnoreCase(anotherString)

public inline fun String.hashCode() : Int = (this as java.lang.String).hashCode()

public inline fun String.indexOf(str : String) : Int = (this as java.lang.String).indexOf(str)

public inline fun String.indexOf(str : String, fromIndex : Int) : Int = (this as java.lang.String).indexOf(str, fromIndex)

public inline fun String.replace(oldChar: Char, newChar : Char) : String = (this as java.lang.String).replace(oldChar, newChar)

public inline fun String.replaceAll(regex: String, replacement : String) : String = (this as java.lang.String).replaceAll(regex, replacement)

public inline fun String.trim() : String = (this as java.lang.String).trim()

public inline fun String.toUpperCase() : String = (this as java.lang.String).toUpperCase()

public inline fun String.toLowerCase() : String = (this as java.lang.String).toLowerCase()

public inline fun String.length() : Int = (this as java.lang.String).length()

public inline fun String.getBytes() : ByteArray = (this as java.lang.String).getBytes()

public inline fun String.toCharArray() : CharArray = (this as java.lang.String).toCharArray()

public fun String.toCharList(): List<Char> = toCharArray().toList()

public inline fun String.format(vararg args : Any?) : String = java.lang.String.format(this, *args)

public inline fun String.split(regex : String) : Array<String> = (this as java.lang.String).split(regex)

public inline fun String.split(ch : Char) : Array<String> = (this as java.lang.String).split(java.util.regex.Pattern.quote(ch.toString()))

public inline fun String.substring(beginIndex : Int) : String = (this as java.lang.String).substring(beginIndex)

public inline fun String.substring(beginIndex : Int, endIndex : Int) : String = (this as java.lang.String).substring(beginIndex, endIndex)

public inline fun String.startsWith(prefix: String) : Boolean = (this as java.lang.String).startsWith(prefix)

public inline fun String.startsWith(prefix: String, toffset: Int) : Boolean = (this as java.lang.String).startsWith(prefix, toffset)

public inline fun String.startsWith(ch: Char) : Boolean = (this as java.lang.String).startsWith(ch.toString())

public inline fun String.contains(seq: CharSequence) : Boolean = (this as java.lang.String).contains(seq)

public inline fun String.endsWith(suffix: String) : Boolean = (this as java.lang.String).endsWith(suffix)

public inline fun String.endsWith(ch: Char) : Boolean = (this as java.lang.String).endsWith(ch.toString())

// "constructors" for String

public inline fun String(bytes : ByteArray, offset : Int, length : Int, charsetName : String) : String = java.lang.String(bytes, offset, length, charsetName) as String

public inline fun String(bytes : ByteArray, offset : Int, length : Int, charset : Charset) : String = java.lang.String(bytes, offset, length, charset) as String

public inline fun String(bytes : ByteArray, charsetName : String) : String = java.lang.String(bytes, charsetName) as String

public inline fun String(bytes : ByteArray, charset : Charset) : String = java.lang.String(bytes, charset) as String

public inline fun String(bytes : ByteArray, i : Int, i1 : Int) : String = java.lang.String(bytes, i, i1) as String

public inline fun String(bytes : ByteArray) : String = java.lang.String(bytes) as String

public inline fun String(chars : CharArray) : String = java.lang.String(chars) as String

public inline fun String(stringBuffer : java.lang.StringBuffer) : String = java.lang.String(stringBuffer) as String

public inline fun String(stringBuilder : java.lang.StringBuilder) : String = java.lang.String(stringBuilder) as String

public inline fun String.replaceFirst(regex : String, replacement : String) : String = (this as java.lang.String).replaceFirst(regex, replacement)

public inline fun String.charAt(index : Int) : Char = (this as java.lang.String).charAt(index)

public inline fun String.split(regex : String, limit : Int) : Array<String> = (this as java.lang.String).split(regex, limit)

public inline fun String.codePointAt(index : Int) : Int = (this as java.lang.String).codePointAt(index)

public inline fun String.codePointBefore(index : Int) : Int = (this as java.lang.String).codePointBefore(index)

public inline fun String.codePointCount(beginIndex : Int, endIndex : Int) : Int = (this as java.lang.String).codePointCount(beginIndex, endIndex)

public inline fun String.compareToIgnoreCase(str : String) : Int = (this as java.lang.String).compareToIgnoreCase(str)

public inline fun String.concat(str : String) : String = (this as java.lang.String).concat(str)

public inline fun String.contentEquals(cs : CharSequence) : Boolean = (this as java.lang.String).contentEquals(cs)

public inline fun String.contentEquals(sb : StringBuffer) : Boolean = (this as java.lang.String).contentEquals(sb)

public inline fun String.getBytes(charset : Charset) : ByteArray = (this as java.lang.String).getBytes(charset)

public inline fun String.getBytes(charsetName : String) : ByteArray = (this as java.lang.String).getBytes(charsetName)

public inline fun String.getChars(srcBegin : Int, srcEnd : Int, dst : CharArray, dstBegin : Int) : Unit = (this as java.lang.String).getChars(srcBegin, srcEnd, dst, dstBegin)

public inline fun String.indexOf(ch : Char) : Int = (this as java.lang.String).indexOf(ch.toString())

public inline fun String.indexOf(ch : Char, fromIndex : Int) : Int = (this as java.lang.String).indexOf(ch.toString(), fromIndex)

public inline fun String.intern() : String = (this as java.lang.String).intern()

public inline fun String.isEmpty() : Boolean = (this as java.lang.String).isEmpty()

public inline fun String.lastIndexOf(ch : Char, fromIndex : Int) : Int = (this as java.lang.String).lastIndexOf(ch.toString(), fromIndex)

public inline fun String.lastIndexOf(str : String, fromIndex : Int) : Int = (this as java.lang.String).lastIndexOf(str, fromIndex)

public inline fun String.matches(regex : String) : Boolean = (this as java.lang.String).matches(regex)

public inline fun String.offsetByCodePoints(index : Int, codePointOffset : Int) : Int = (this as java.lang.String).offsetByCodePoints(index, codePointOffset)

public inline fun String.regionMatches(ignoreCase : Boolean, toffset : Int, other : String, ooffset : Int, len : Int) : Boolean = (this as java.lang.String).regionMatches(ignoreCase, toffset, other, ooffset, len)

public inline fun String.regionMatches(toffset : Int, other : String, ooffset : Int, len : Int) : Boolean = (this as java.lang.String).regionMatches(toffset, other, ooffset, len)

public inline fun String.replace(target : CharSequence, replacement : CharSequence) : String = (this as java.lang.String).replace(target, replacement)

public inline fun String.subSequence(beginIndex : Int, endIndex : Int) : CharSequence = (this as java.lang.String).subSequence(beginIndex, endIndex)

public inline fun String.toLowerCase(locale : java.util.Locale) : String = (this as java.lang.String).toLowerCase(locale)

public inline fun String.toUpperCase(locale : java.util.Locale) : String = (this as java.lang.String).toUpperCase(locale)


public inline fun CharSequence.charAt(index : Int) : Char = (this as java.lang.CharSequence).charAt(index)

public fun CharSequence.get(index : Int) : Char = charAt(index)

public inline fun CharSequence.subSequence(start : Int, end : Int) : CharSequence? = (this as java.lang.CharSequence).subSequence(start, end)

public fun CharSequence.get(start : Int, end : Int) : CharSequence? = subSequence(start, end)

public inline fun CharSequence.toString() : String? = (this as java.lang.CharSequence).toString()

public inline fun CharSequence.length() : Int = (this as java.lang.CharSequence).length()


public fun String.toByteArray(encoding: String = Charset.defaultCharset().name()): ByteArray = (this as java.lang.String).getBytes(encoding)
public inline fun String.toByteArray(encoding: Charset): ByteArray =  (this as java.lang.String).getBytes(encoding)

public inline fun String.toBoolean() : Boolean = java.lang.Boolean.parseBoolean(this)
public inline fun String.toShort() : Short = java.lang.Short.parseShort(this)
public inline fun String.toInt() : Int = java.lang.Integer.parseInt(this)
public inline fun String.toLong() : Long = java.lang.Long.parseLong(this)
public inline fun String.toFloat() : Float = java.lang.Float.parseFloat(this)
public inline fun String.toDouble() : Double = java.lang.Double.parseDouble(this)

/**
 * Converts the string into a regular expression [[Pattern]] optionally
 * with the specified flags from [[Pattern]] or'd together
 * so that strings can be split or matched on.
 */
public fun String.toRegex(flags: Int=0): java.util.regex.Pattern {
    return java.util.regex.Pattern.compile(this, flags)
}

val String.reader : StringReader
get() = StringReader(this)

val String.size : Int
get() = length()


/**
 * Returns a copy of this string capitalised if it is not empty or already starting with an uppper case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/StringTest.kt capitalize
 */
public fun String.capitalize(): String {
    return if (isNotEmpty() && charAt(0).isLowerCase()) substring(0, 1).toUpperCase() + substring(1) else this
}

/**
 * Returns a copy of this string with the first letter lower case if it is not empty or already starting with a lower case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/StringTest.kt decapitalize
 */
public fun String.decapitalize(): String {
    return if (isNotEmpty() && charAt(0).isUpperCase()) substring(0, 1).toLowerCase() + substring(1) else this
}

/**
 * Repeats a given string n times.
 * When n < 0, IllegalArgumentException is thrown.
 * @includeFunctionBody ../../test/StringTest.kt repeat
 */
public fun String.repeat(n: Int): String {
    require(n >= 0, { "Cannot repeat string $n times" })

    var sb = StringBuilder()
    for (i in 1..n) {
        sb.append(this)
    }
    return sb.toString()
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
  * Reverses order of characters in a string
  *
  * @includeFunctionBody ../../test/StringTest.kt reverse
  */
public fun String.reverse(): String = StringBuilder(this).reverse().toString()

/**
 * Performs the given *operation* on each character
 *
 * @includeFunctionBody ../../test/StringTest.kt forEach
 */
public inline fun String.forEach(operation: (Char) -> Unit) { for(c in this) operation(c) }

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
public fun String.appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
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
 *  Returns a new List containing the results of applying the given *transform* function to each character in this string
 *
 */
public inline fun <R> String.map(transform: (Char) -> R): List<R> = mapTo(ArrayList<R>(), transform)

/**
 * Transforms each character of this string with the given *transform* function and
 * adds each return value to the given *result* collection
 *
 */
public inline fun <R, C: MutableCollection<in R>> String.mapTo(result: C, transform: (Char) -> R): C {
    for (c in this) result.add(transform(c))
    return result
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
public fun String.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
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
public fun String.drop(n: Int): String = dropWhile(countTo(n))

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
public fun String.take(n: Int): String = takeWhile(countTo(n))

/** Copies all characters into the given collection */
public fun <C: MutableCollection<in Char>> String.toCollection(result: C): C {
    for (c in this) result.add(c)
    return result
}

/** Copies all characters into a [[LinkedList]]  */
public fun String.toLinkedList(): LinkedList<Char> = toCollection(LinkedList<Char>())

/** Copies all characters into a [[List]] */
public fun String.toList(): List<Char> = toCollection(ArrayList<Char>(this.length()))

/** Copies all characters into a [[Collection] */
public fun String.toCollection(): Collection<Char> = toCollection(ArrayList<Char>(this.length()))

/** Copies all characters into a [[Set]] */
public fun String.toSet(): Set<Char> = toCollection(HashSet<Char>())

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
public fun String.replaceAll(regexp: String, body: (java.util.regex.MatchResult) -> String) : String {
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

