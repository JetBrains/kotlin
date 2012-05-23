// NOTE this file is auto-generated from src/kotlin/JLangIterablesLazy.kt
package kotlin

import kotlin.util.*

import java.util.ArrayList
import java.util.Collection
import java.util.List

//
// This file contains methods which could have a lazy implementation for things like
// Iterator<Char> or java.util.Iterator<Char>
//
// See [[GenerateStandardLib.kt]] for more details
//

/**
 * Returns a list containing all elements which match the given *predicate*
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filter
 */
public inline fun CharArray.filter(predicate: (Char) -> Boolean) : List<Char> = filterTo(ArrayList<Char>(), predicate)

/**
 * Returns a list containing all elements which do not match the given predicate
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNot
 */
public inline fun CharArray.filterNot(predicate: (Char)-> Boolean) : List<Char> = filterNotTo(ArrayList<Char>(), predicate)

/**
 * Returns a list containing all the non-*null* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNotNull
 */
public inline fun CharArray?.filterNotNull() : List<Char> = filterNotNullTo<ArrayList<Char>>(java.util.ArrayList<Char>())

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 *
 * @includeFunctionBody ../../test/CollectionTest.kt flatMap
 */
public inline fun <R> CharArray.flatMap(transform: (Char)-> Collection<R>) : Collection<R> = flatMapTo(ArrayList<R>(), transform)

/**
 * Creates a copy of this collection as a [[List]] with the element added at the end
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plus
 */
public inline fun  CharArray.plus(element: Char): List<Char> {
    val list = toCollection(ArrayList<Char>())
    list.add(element)
    return list
}


/**
 * Creates a copy of this collection as a [[List]] with all the elements added at the end
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plusCollection
 */
public inline fun  CharArray.plus(elements: CharArray): List<Char> {
    val list = toCollection(ArrayList<Char>())
    list.addAll(elements.toCollection())
    return list
}

/**
 * Returns a list containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt requireNoNulls
 */
public inline fun  CharArray?.requireNoNulls() : List<Char> {
    val list = ArrayList<Char>()
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
public inline fun CharArray.drop(n: Int): List<Char> {
    fun countTo(n: Int): (Char) -> Boolean {
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
public inline fun CharArray.dropWhile(predicate: (Char) -> Boolean): List<Char> = dropWhileTo(ArrayList<Char>(), predicate)

/**
 * Returns a list containing the first *n* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt take
 */
public inline fun CharArray.take(n: Int): List<Char> {
    fun countTo(n: Int): (Char) -> Boolean {
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
public inline fun CharArray.takeWhile(predicate: (Char) -> Boolean): List<Char> = takeWhileTo(ArrayList<Char>(), predicate)
