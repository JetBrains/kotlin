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
public inline fun <T> Array<T>.filter(predicate: (T) -> Boolean) : List<T> = filterTo(ArrayList<T>(), predicate)

/**
 * Returns a list containing all elements which do not match the given predicate
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNot
 */
public inline fun <T> Array<T>.filterNot(predicate: (T)-> Boolean) : List<T> = filterNotTo(ArrayList<T>(), predicate)

/**
 * Returns a list containing all the non-*null* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt filterNotNull
 */
public inline fun <T> Array<T?>?.filterNotNull() : List<T> = filterNotNullTo<T, ArrayList<T>>(java.util.ArrayList<T>())

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 *
 * @includeFunctionBody ../../test/CollectionTest.kt flatMap
 */
public inline fun <T, R> Array<T>.flatMap(transform: (T)-> Collection<R>) : Collection<R> = flatMapTo(ArrayList<R>(), transform)

/**
 * Returns a list containing the first *n* elements
 *
 * @includeFunctionBody ../../test/CollectionTest.kt take
 */
public inline fun <T> Array<T>.take(n: Int): List<T> {
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
public inline fun <T> Array<T>.takeWhile(predicate: (T) -> Boolean): List<T> = takeWhileTo(ArrayList<T>(), predicate)
