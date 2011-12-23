// NOTE this file is auto-generated from stdlib/ktSrc/JavaIterables.kt
package std
// Note: this file is used to generate methods on Array<T> too

import java.util.*

/** Returns true if any elements in the collection match the given predicate */
inline fun <T> Array<T>.any(predicate: (T)-> Boolean) : Boolean {
  for (elem in this) {
    if (predicate(elem)) {
      return true
    }
  }
  return false
}

/** Returns true if all elements in the collection match the given predicate */
inline fun <T> Array<T>.all(predicate: (T)-> Boolean) : Boolean {
  for (elem in this) {
    if (!predicate(elem)) {
      return false
    }
  }
  return true
}

/** Returns the first item in the collection which matches the given predicate or null if none matched */
inline fun <T> Array<T>.find(predicate: (T)-> Boolean) : T? {
  for (elem in this) {
    if (predicate(elem))
      return elem
  }
  return null
}

/** Returns a new collection containing all elements in this collection which match the given predicate */
inline fun <T> Array<T>.filter(result: Collection<T> = ArrayList<T>(), predicate: (T)-> Boolean) : Collection<T> {
  for (elem in this) {
    if (predicate(elem))
      result.add(elem)
  }
  return result
}

/**
  * Returns the result of transforming each item in the collection to a one or more values which
  * are concatenated together into a single collection
  */
inline fun <T, out R> Array<T>.flatMap(result: Collection<R> = ArrayList<R>(), transform: (T)-> Collection<R>) : Collection<R> {
  for (elem in this) {
    val coll = transform(elem)
    if (coll != null) {
      for (r in coll) {
        result.add(r)
      }
    }
  }
  return result
}

/** Performs the given operation on each element inside the collection */
inline fun <T> Array<T>.foreach(operation: (element: T) -> Unit) {
  for (elem in this)
    operation(elem)
}

/** Creates a String from all the elements in the collection, using the seperator between them and using the given prefix and postfix if supplied */
inline fun <T> Array<T>.join(separator: String, prefix: String = "", postfix: String = "") : String {
  val buffer = StringBuilder(prefix)
  var first = true
  for (elem in this) {
    if (first)
      first = false
    else
      buffer.append(separator)
    buffer.append(elem)
  }
  buffer.append(postfix)
  return buffer.toString().sure()
}

/** Returns a new collection containing the results of applying the given function to each element in this collection */
/*
inline fun <T, R> Array<T>.map(result: Collection<R> = ArrayList<R>(), transform : (T) -> R) : Collection<R> {
  for (item in this)
    result.add(transform(item))
  return result
}
*/

/** Returns a new collection containing the results of applying the given function to each element in this collection */
inline fun <T, R> Array<T>.map(result: Collection<R> = ArrayList<R>(this.size), transform : (T) -> R) : Collection<R> {
  for (item in this)
    result.add(transform(item))
  return result
}

inline fun <T, C: Collection<T>> Array<T>.to(result: C) : C {
  for (elem in this)
    result.add(elem)
  return result
}

inline fun <T> Array<T>.toLinkedList() : LinkedList<T> = this.to(LinkedList<T>())

inline fun <T> Array<T>.toList() : List<T> = this.to(ArrayList<T>())

inline fun <T> Array<T>.toSet() : Set<T> = this.to(HashSet<T>())

/**
  TODO figure out necessary variance/generics ninja stuff... :)
inline fun <in T> Array<T>.toSortedList(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
  val answer = this.toList()
  answer.sort(transform)
  return answer
}
*/
