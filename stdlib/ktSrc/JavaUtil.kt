namespace std.util

import java.util.*

/** Returns the size of the collection */
val Collection<*>.size : Int
    get() = size()

/** Returns true if this collection is empty */
val Collection<*>.empty : Boolean
    get() = isEmpty()

/** Returns a new ArrayList with a variable number of initial elements */
inline fun arrayList<T>(vararg values: T) : ArrayList<T> = values.to(ArrayList<T>(values.size))

/** Returns a new LinkedList with a variable number of initial elements */
inline fun linkedList<T>(vararg values: T) : LinkedList<T>  = values.to(LinkedList<T>())

/** Returns a new HashSet with a variable number of initial elements */
inline fun hashSet<T>(vararg values: T) : HashSet<T> = values.to(HashSet<T>(values.size))

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

// TODO would be nice to not have to write extension methods for Array, Iterable and Iterator

inline fun <T, C: Collection<T>> Array<T>.to(result: C) : C {
  for (elem in this)
    result.add(elem)
  return result
}

inline fun <T, C: Collection<T>> java.lang.Iterable<T>.to(result: C) : C {
  for (elem in this)
    result.add(elem)
  return result
}

inline fun <T> java.lang.Iterable<T>.toLinkedList() : LinkedList<T> = this.to(LinkedList<T>())

inline fun <T> java.lang.Iterable<T>.toList() : List<T> = this.to(ArrayList<T>())

inline fun <T> java.lang.Iterable<T>.toSet() : Set<T> = this.to(HashSet<T>())

inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList() : List<T> = toList().sort()

inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList(comparator: java.util.Comparator<T>) : List<T> = toList().sort(comparator)

/**
  TODO figure out necessary variance/generics ninja stuff... :)
inline fun <in T> java.lang.Iterable<T>.toSortedList(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
  val answer = this.toList()
  answer.sort(transform)
  return answer
}
*/

inline fun <T> java.util.Collection<T>.toArray() : Array<T> {
  val answer = Array<T>(this.size)
  var idx = 0
  for (elem in this)
    answer[idx++] = elem
  return answer as Array<T>
}


// List APIs

inline fun <in T: java.lang.Comparable<T>> List<T>.sort() : List<T> {
  Collections.sort(this)
  return this
}

inline fun <in T: java.lang.Comparable<T>> List<T>.sort(comparator: java.util.Comparator<T>) : List<T> {
  Collections.sort(this, comparator)
  return this
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



// Map APIs
/** Returns the size of the map */
/* TODO get redeclaration errors
val Map<*,*>.size : Int
    get() = size()
*/

/** Returns true if this map is empty */
/* TODO get redeclaration errors
val Map<*,*>.empty : Boolean
    get() = isEmpty()
*/

/** Returns the value for the given key or returns the result of the defaultValue function if there was no entry for the given key */
inline fun <K,V> java.util.Map<K,V>.getOrElse(key: K, defaultValue: fun(): V) : V {
  val current = this.get(key)
  if (current != null) {
    return current
  } else {
    return defaultValue()
  }
}

/** Returns the value for the given key or the result of the defaultValue function is put into the map for the given value and returned */
inline fun <K,V> java.util.Map<K,V>.getOrElseUpdate(key: K, defaultValue: fun(): V) : V {
  val current = this.get(key)
  if (current != null) {
    return current
  } else {
    val answer = defaultValue()
    this.put(key, answer)
    return answer
  }
}
