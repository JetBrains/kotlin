package kotlin

import java.util.*

//
// This file contains methods which are optimised for working on Collection / Array collections where the size
// could be used to implement a more optimal solution
//
// See [[GenerateStandardLib.kt]] for more details
//

/**
 * Returns a new List containing the results of applying the given *transform* function to each element in this collection
 *
 * @includeFunctionBody ../../test/CollectionTest.kt map
 */
public inline fun <T, R> java.util.Collection<T>.map(transform : (T) -> R) : java.util.List<R> {
    return mapTo(java.util.ArrayList<R>(this.size), transform)
}

/**
 * Transforms each element of this collection with the given *transform* function and
 * adds each return value to the given *results* collection
 */
public inline fun <T, R, C: Collection<in R>> java.util.Collection<T>.mapTo(result: C, transform : (T) -> R) : C {
  for (item in this)
    result.add(transform(item))
  return result
}
