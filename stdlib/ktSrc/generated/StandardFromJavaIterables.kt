// NOTE this file is auto-generated from stdlib/ktSrc/JavaIterables.kt
package std

import java.util.*

/** Returns true if any elements in the collection match the given predicate */
inline fun <T> Iterable<T>.any(predicate: (T)-> Boolean) : Boolean {
  for (elem in this) {
    if (predicate(elem)) {
      return true
    }
  }
  return false
}

/** Returns true if all elements in the collection match the given predicate */
inline fun <T> Iterable<T>.all(predicate: (T)-> Boolean) : Boolean {
  for (elem in this) {
    if (!predicate(elem)) {
      return false
    }
  }
  return true
}

/** Returns the first item in the collection which matches the given predicate or null if none matched */
inline fun <T> Iterable<T>.find(predicate: (T)-> Boolean) : T? {
  for (elem in this) {
    if (predicate(elem))
      return elem
  }
  return null
}

/** Returns a new collection containing all elements in this collection which match the given predicate */
inline fun <T> Iterable<T>.filter(result: Collection<T> = ArrayList<T>(), predicate: (T)-> Boolean) : Collection<T> {
  for (elem in this) {
    if (predicate(elem))
      result.add(elem)
  }
  return result
}

/** Returns a new collection containing all elements in this collection which do not match the given predicate */
inline fun <T> Iterable<T>.filterNot(result: Collection<T> = ArrayList<T>(), predicate: (T)-> Boolean) : Collection<T> {
  for (elem in this) {
    if (!predicate(elem))
      result.add(elem)
  }
  return result
}

/**
  * Returns the result of transforming each item in the collection to a one or more values which
  * are concatenated together into a single collection
  */
inline fun <T, out R> Iterable<T>.flatMap(result: Collection<R> = ArrayList<R>(), transform: (T)-> Collection<R>) : Collection<R> {
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
inline fun <T> Iterable<T>.foreach(operation: (element: T) -> Unit) {
  for (elem in this)
    operation(elem)
}

/** Creates a String from all the elements in the collection, using the seperator between them and using the given prefix and postfix if supplied */
inline fun <T> Iterable<T>.join(separator: String, prefix: String = "", postfix: String = "") : String {
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

inline fun <T, C: Collection<T>> Iterable<T>.to(result: C) : C {
  for (elem in this)
    result.add(elem)
  return result
}

inline fun <T> Iterable<T>.toLinkedList() : LinkedList<T> = this.to(LinkedList<T>())

inline fun <T> Iterable<T>.toList() : List<T> = this.to(ArrayList<T>())

inline fun <T> Iterable<T>.toSet() : Set<T> = this.to(HashSet<T>())

/**
  TODO figure out necessary variance/generics ninja stuff... :)
inline fun <in T> Iterable<T>.toSortedList(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
  val answer = this.toList()
  answer.sort(transform)
  return answer
}
*/
