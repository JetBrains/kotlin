namespace std.util

import java.util.*

/** Returns the size of the collection */
val Collection<*>.size : Int
    get() = size()

/** Returns true if this collection is empty */
val Collection<*>.empty : Boolean
    get() = isEmpty()

/** Returns a new ArrayList with a variable number of initial elements */
inline fun arrayList<T>(vararg values: T) : ArrayList<T> {
  val answer = ArrayList<T>()
  for (v in values)
    answer.add(v)
  return answer;
}

/** Returns a new LinkedList with a variable number of initial elements */
inline fun linkedList<T>(vararg values: T) : LinkedList<T> {
  val answer = LinkedList<T>()
  for (v in values)
    answer.add(v)
  return answer;
}

/** Returns a new HashSet with a variable number of initial elements */
inline fun hashSet<T>(vararg values: T) : HashSet<T> {
  val answer = HashSet<T>()
  for (v in values)
    answer.add(v)
  return answer;
}

/** Returns true if any elements in the collection match the given predicate */
inline fun <T> java.lang.Iterable<T>.any(predicate: fun(T): Boolean) : Boolean {
  for (elem in this) {
    if (predicate(elem)) {
      return true
    }
  }
  return false
}

/** Returns true if all elements in the collection match the given predicate */
inline fun <T> java.lang.Iterable<T>.all(predicate: fun(T): Boolean) : Boolean {
  for (elem in this) {
    if (!predicate(elem)) {
      return false
    }
  }
  return true
}

/** Returns the first item in the collection which matches the given predicate or null if none matched */
inline fun <T> java.lang.Iterable<T>.find(predicate: fun(T): Boolean) : T? {
  for (elem in this) {
    if (predicate(elem))
      return elem
  }
  return null
}

/** Returns a new collection containing all elements in this collection which match the given predicate */
inline fun <T> java.lang.Iterable<T>.filter(result: Collection<T> = ArrayList<T>(), predicate: fun(T): Boolean) : Collection<T> {
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
inline fun <T, out R> java.lang.Iterable<T>.flatMap(result: Collection<R> = ArrayList<R>(), transform: fun(T): Collection<R>) : Collection<R> {
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
inline fun <T> java.lang.Iterable<T>.foreach(operation: fun(element: T) : Unit) {
  for (elem in this)
    operation(elem)
}

/** Creates a String from all the elements in the collection, using the seperator between them and using the given prefix and postfix if supplied */
inline fun <T> java.lang.Iterable<T>.join(separator: String, prefix: String = "", postfix: String = "") : String {
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
inline fun <T, R> java.lang.Iterable<T>.map(result: Collection<R> = ArrayList<R>(), transform : fun(T) : R) : Collection<R> {
  for (item in this)
    result.add(transform(item))
  return result
}

/** Returns a new collection containing the results of applying the given function to each element in this collection */
inline fun <T, R> java.util.Collection<T>.map(result: Collection<R> = ArrayList<R>(this.size), transform : fun(T) : R) : Collection<R> {
  for (item in this)
    result.add(transform(item))
  return result
}

inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList() : List<T> {
  val answer = this.toList()
  answer.sort()
  return answer
}

inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList(comparator: java.util.Comparator<T>) : List<T> {
  val answer = this.toList()
  answer.sort(comparator)
  return answer
}

/**
  TODO figure out necessary variance/generics ninja stuff... :)
inline fun <in T> java.lang.Iterable<T>.toSortedList(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
  val answer = this.toList()
  answer.sort(transform)
  return answer
}
*/

inline fun <T> java.lang.Iterable<T>.toList() : List<T> {
  if (this is List<T>)
    return this
  else {
    val list = ArrayList<T>()
    for (elem in this)
      list.add(elem)
    return list
  }
}

inline fun <T> java.util.Collection<T>.toArray() : Array<T> {
  if (this is Array<T>)
    return this
  else {
    val answer = Array<T>(this.size)
    var idx = 0
    for (elem in this)
      answer[idx++] = elem
    return answer as Array<T>
  }
}


// List APIs

inline fun <in T: java.lang.Comparable<T>> List<T>.sort() : Unit {
  Collections.sort(this)
}

inline fun <in T: java.lang.Comparable<T>> List<T>.sort(comparator: java.util.Comparator<T>) : Unit {
  Collections.sort(this, comparator)
}

/**
  TODO figure out necessary variance/generics ninja stuff... :)
inline fun <in T> List<T>.sort(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
  val comparator = java.util.Comparator<T>() {
    fun compare(o1: T, o2: T): Int {
      val v1 = transform(o1)
      val v2 = transform(o2)
      if (v1 == v2) {
        return 0
      } else {
        return v1.compareTo(v2)
      }
    }
  }
  answer.sort(comparator)
}
*/

val <T> List<T>.head : T?
    get() = this.get(0)

val <T> List<T>.first : T?
    get() = this.head

val <T> List<T>.tail : T?
    get() {
      val s = this.size
      return if (s > 0) this.get(s - 1) else null

    }
val <T> List<T>.last : T?
  get() = this.tail

