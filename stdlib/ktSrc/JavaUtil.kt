package std.util

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


inline fun <T> java.util.Collection<T>.toArray() : Array<T> {
  val answer = Array<T>(this.size)
  var idx = 0
  for (elem in this)
    answer[idx++] = elem
  return answer as Array<T>
}

/** TODO these functions don't work when they generate the Array<T> versions when they are in JavaIterables */
inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList() : List<T> = toList().sort()

inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList(comparator: java.util.Comparator<T>) : List<T> = toList().sort(comparator)



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
inline fun <K,V> java.util.Map<K,V>.getOrElse(key: K, defaultValue: ()-> V) : V {
  val current = this.get(key)
  if (current != null) {
    return current
  } else {
    return defaultValue()
  }
}

/** Returns the value for the given key or the result of the defaultValue function is put into the map for the given value and returned */
inline fun <K,V> java.util.Map<K,V>.getOrElseUpdate(key: K, defaultValue: ()-> V) : V {
  val current = this.get(key)
  if (current != null) {
    return current
  } else {
    val answer = defaultValue()
    this.put(key, answer)
    return answer
  }
}
