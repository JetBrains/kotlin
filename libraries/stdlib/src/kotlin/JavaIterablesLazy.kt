package kotlin

import java.util.ArrayList
import java.util.Collection
import java.util.List

//
// This file contains methods which could have a lazy implementation for things like
// Iterator<T> or java.util.Iterator<T>
//
// See [[GenerateStandardLib.kt]] for more details
//

/**
 * Returns a list containing all elements which match the given *predicate*
 *
 * @includeFunction ../../test/CollectionTest.kt filter
 */
public inline fun <T> java.lang.Iterable<T>.filter(predicate: (T) -> Boolean) : List<T> = filterTo(ArrayList<T>(), predicate)

/**
 * Returns a list containing all elements which do not match the given predicate
 *
 * @includeFunction ../../test/CollectionTest.kt filterNot
 */
public inline fun <T> java.lang.Iterable<T>.filterNot(predicate: (T)-> Boolean) : List<T> = filterNotTo(ArrayList<T>(), predicate)

/**
 * Returns a list containing all the non-*null* elements
 *
 * @includeFunction ../../test/CollectionTest.kt filterNotNull
 */
public inline fun <T> java.lang.Iterable<T?>?.filterNotNull() : List<T> = filterNotNullTo<T, ArrayList<T>>(java.util.ArrayList<T>())

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 *
 * @includeFunction ../../test/CollectionTest.kt flatMap
 */
public inline fun <T, R> java.lang.Iterable<T>.flatMap(transform: (T)-> Collection<R>) : Collection<R> = flatMapTo(ArrayList<R>(), transform)

/**
 * Returns a list containing the first *n* elements
 *
 * @includeFunction ../../test/CollectionTest.kt take
 */
public inline fun <T> java.lang.Iterable<T>.take(n: Int): List<T> {
    fun countTo(n: Int): (T) -> Boolean {
      var count = 0
      return { ++count; count <= n }
    }
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 *
 * @includeFunction ../../test/CollectionTest.kt takeWhile
 */
public inline fun <T> java.lang.Iterable<T>.takeWhile(predicate: (T) -> Boolean): List<T> = takeWhileTo(ArrayList<T>(), predicate)

/**
 * Creates a string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied
 *
 * @includeFunction ../../test/CollectionTest.kt join
 */
public inline fun <T> java.lang.Iterable<T>.join(separator: String = ", ", prefix: String = "", postfix: String = "") : String {
    val buffer = StringBuilder(prefix)
    var first = true
    for (element in this) {
        if (first) first = false else buffer.append(separator)
        buffer.append(element)
    }
    buffer.append(postfix)
    return buffer.toString().sure()
}
