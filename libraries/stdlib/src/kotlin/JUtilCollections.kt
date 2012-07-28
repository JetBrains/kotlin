package kotlin

import java.util.*

//
// This file contains methods which are optimised for working on Collection / Array collections where the size
// could be used to implement a more optimal solution
//
// See [[GenerateStandardLib.kt]] for more details
//

/**
 * Transforms each element of this collection with the given *transform* function and
 * adds each return value to the given *results* collection
 */
public inline fun <T, R, C: Collection<in R>> java.util.Collection<T>.mapTo(result: C, transform : (T) -> R) : C {
  for (item in this)
    result.add(transform(item))
  return result
}
