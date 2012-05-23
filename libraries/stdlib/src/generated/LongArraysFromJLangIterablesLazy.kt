// NOTE this file is auto-generated from src/kotlin/JLangIterablesLazy.kt
package kotlin

import kotlin.util.*

import java.util.ArrayList
import java.util.Collection
import java.util.List

//
// This file contains methods which could have a lazy implementation for things like
// Iterator<Long> or java.util.Iterator<Long>
//
// See [[GenerateStandardLib.kt]] for more details
//

/**
 * Returns a list containing all elements which match the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filter
 */
public inline fun LongArray.filter(predicate: (Long) -> Boolean) : List<Long> = filterTo(ArrayList<Long>(), predicate)

/**
 * Returns a list containing all elements which do not match the given predicate
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNot
 */
public inline fun LongArray.filterNot(predicate: (Long)-> Boolean) : List<Long> = filterNotTo(ArrayList<Long>(), predicate)

/**
 * Returns a list containing all the non-*null* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNotNull
 */
public inline fun LongArray?.filterNotNull() : List<Long> = filterNotNullTo<ArrayList<Long>>(java.util.ArrayList<Long>())

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 *
 * @includeFunctionBody ../../test/CollectionTest.kt flatMap
 */
public inline fun <R> LongArray.flatMap(transform: (Long)-> Collection<R>) : Collection<R> = flatMapTo(ArrayList<R>(), transform)

/**
 * Creates a copy of this collection as a [[List]] with the element added at the end
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plus
 */
public inline fun  LongArray.plus(element: Long): List<Long> {
    val list = toCollection(ArrayList<Long>())
    list.add(element)
    return list
}


/**
 * Creates a copy of this collection as a [[List]] with all the elements added at the end
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plusCollection
 */
public inline fun  LongArray.plus(elements: LongArray): List<Long> {
    val list = toCollection(ArrayList<Long>())
    list.addAll(elements.toCollection())
    return list
}

/**
 * Returns a list containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt requireNoNulls
 */
public inline fun  LongArray?.requireNoNulls() : List<Long> {
    val list = ArrayList<Long>()
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
public inline fun LongArray.drop(n: Int): List<Long> {
    fun countTo(n: Int): (Long) -> Boolean {
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
public inline fun LongArray.dropWhile(predicate: (Long) -> Boolean): List<Long> = dropWhileTo(ArrayList<Long>(), predicate)

/**
 * Returns a list containing the first *n* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt take
 */
public inline fun LongArray.take(n: Int): List<Long> {
    fun countTo(n: Int): (Long) -> Boolean {
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
public inline fun LongArray.takeWhile(predicate: (Long) -> Boolean): List<Long> = takeWhileTo(ArrayList<Long>(), predicate)
