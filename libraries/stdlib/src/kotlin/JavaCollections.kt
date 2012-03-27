package kotlin

import java.util.*

/** Returns a new List containing the results of applying the given function to each element in this collection */
inline fun <T, R> java.util.Collection<T>.map(transform : (T) -> R) : java.util.List<R> {
    return mapTo(java.util.ArrayList<R>(this.size), transform)
}

/** Transforms each element of this collection with the given function then adds the results to the given collection */
inline fun <T, R, C: Collection<in R>> java.util.Collection<T>.mapTo(result: C, transform : (T) -> R) : C {
  for (item in this)
    result.add(transform(item))
  return result
}
