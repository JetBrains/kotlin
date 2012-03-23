// NOTE this file is auto-generated from src/JavaCollections.kt
package kotlin.util

import java.util.*

/** Returns a new List containing the results of applying the given function to each element in this collection */
inline fun <T, R> java.lang.Iterable<T>.map(transform : (T) -> R) : java.util.List<R> {
    return mapTo(java.util.ArrayList<R>(), transform)
}

/** Transforms each element of this collection with the given function then adds the results to the given collection */
inline fun <T, R, C: Collection<in R>> java.lang.Iterable<T>.mapTo(result: C, transform : (T) -> R) : C {
  for (item in this)
    result.add(transform(item))
  return result
}
