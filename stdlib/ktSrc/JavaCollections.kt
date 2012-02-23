package std.util

import java.util.*

/** Returns a new collection containing the results of applying the given function to each element in this collection */
inline fun <T, R> java.util.Collection<T>.map(result: Collection<R> = ArrayList<R>(this.size), transform : (T) -> R) : Collection<R> {
  for (item in this)
    result.add(transform(item))
  return result
}

/** Returns true if the collection is not empty */
inline fun <T> java.util.Collection<T>.notEmpty() : Boolean = !this.isEmpty()