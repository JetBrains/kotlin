// NOTE this file is auto-generated from src/kotlin/JLangIterablesLazy.kt
package kotlin

import kotlin.util.*

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
 * @includeFunctionBody ../../test/CollectionTest.kt filter
 */
public inline fun <T> Iterable<T>.filter(predicate: (T) -> Boolean) : List<T> = filterTo(ArrayList<T>(), predicate)

/**
 * Returns a list containing all elements which do not match the given predicate
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNot
 */
public inline fun <T> Iterable<T>.filterNot(predicate: (T)-> Boolean) : List<T> = filterNotTo(ArrayList<T>(), predicate)

/**
 * Returns a list containing all the non-*null* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNotNull
 */
public inline fun <T> Iterable<T?>?.filterNotNull() : List<T> = filterNotNullTo<T, ArrayList<T>>(java.util.ArrayList<T>())

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 *
 * @includeFunctionBody ../../test/CollectionTest.kt flatMap
 */
public inline fun <T, R> Iterable<T>.flatMap(transform: (T)-> Collection<R>) : Collection<R> = flatMapTo(ArrayList<R>(), transform)

/**
 * Creates a copy of this collection as a [[List]] with the element added at the end
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plus
 */
public inline fun <in T> Iterable<T>.plus(element: T): List<in T> {
    val list = toCollection(ArrayList<T>())
    list.add(element)
    return list
}


/**
 * Creates a copy of this collection as a [[List]] with all the elements added at the end
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plusCollection
 */
public inline fun <in T> Iterable<T>.plus(elements: Iterable<T>): List<T> {
    val list = toCollection(ArrayList<T>())
    list.addAll(elements.toCollection())
    return list
}

/**
 * Returns a list containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt requireNoNulls
 */
public inline fun <in T> Iterable<T?>?.requireNoNulls() : List<T> {
    val list = ArrayList<T>()
    for (element in this) {
        if (element == null) {
            throw IllegalArgumentException("null element found in $this")
        } else {
            list.add(element)
        }
    }
    return list
}

/**
 * Returns a list containing everything but the first *n* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt drop
 */
public inline fun <T> Iterable<T>.drop(n: Int): List<T> {
    fun countTo(n: Int): (T) -> Boolean {
      var count = 0
      return { ++count; count <= n }
    }
    return dropWhile(countTo(n))
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt dropWhile
 */
public inline fun <T> Iterable<T>.dropWhile(predicate: (T) -> Boolean): List<T> = dropWhileTo(ArrayList<T>(), predicate)

/**
 * Returns a list containing the first *n* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt take
 */
public inline fun <T> Iterable<T>.take(n: Int): List<T> {
    fun countTo(n: Int): (T) -> Boolean {
      var count = 0
      return { ++count; count <= n }
    }
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt takeWhile
 */
public inline fun <T> Iterable<T>.takeWhile(predicate: (T) -> Boolean): List<T> = takeWhileTo(ArrayList<T>(), predicate)
