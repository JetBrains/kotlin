package kotlin

import java.util.*

/** Returns the size of the collection */
val Collection<*>.size : Int
    get() = size()

/** Returns true if this collection is empty */
val Collection<*>.empty : Boolean
    get() = isEmpty()

val Collection<*>.indices : IntRange
    get() = 0..size-1

/**
 * Converts the collection to an array
 */
public inline fun <T> java.util.Collection<T>.toArray() : Array<T> {
  val answer = arrayOfNulls<T>(this.size)
  var idx = 0
  for (elem in this)
    answer[idx++] = elem
  return answer as Array<T>
}

/** Returns true if the collection is not empty */
public inline fun <T> java.util.Collection<T>.notEmpty() : Boolean = !this.isEmpty()

/** Returns the Collection if its not null otherwise it returns the empty list */
public inline fun <T> java.util.Collection<T>?.orEmpty() : Collection<T>
    = if (this != null) this else Collections.emptyList<T>() as Collection<T>


/** TODO these functions don't work when they generate the Array<T> versions when they are in JLIterables */
public inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList() : List<T> = toList().sort()

public inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList(comparator: java.util.Comparator<T>) : List<T> = toList().sort(comparator)


// List APIs

/**
 * Reverses the order the elements into a list
 *
 * @includeFunctionBody ../../test/CollectionTest.kt reverse
 */
public inline fun <T> List<T>.reverse() : List<T> {
    Collections.reverse(this)
    return this
}

public inline fun <in T: java.lang.Comparable<T>> List<T>.sort() : List<T> {
  Collections.sort(this)
  return this
}

public inline fun <in T> List<T>.sort(comparator: java.util.Comparator<T>) : List<T> {
  Collections.sort(this, comparator)
  return this
}

/** Returns the List if its not null otherwise returns the empty list */
public inline fun <T> java.util.List<T>?.orEmpty() : java.util.List<T>
    = if (this != null) this else Collections.emptyList<T>() as java.util.List<T>

/**
  TODO figure out necessary variance/generics ninja stuff... :)
public inline fun <in T> List<T>.sort(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
  val comparator = java.util.Comparator<T>() {
    public fun compare(o1: T, o2: T): Int {
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

/**
 * Returns the first item in the list
 *
 * @includeFunctionBody ../../test/ListTest.kt first
 */
val <T> List<T>.first : T?
    get() = this.head


/**
 * Returns the last item in the list
 *
 * @includeFunctionBody ../../test/ListTest.kt last
 */
val <T> List<T>.last : T?
    get() {
      val s = this.size
      return if (s > 0) this.get(s - 1) else null
    }

/**
 * Returns the index of the last item in the list or -1 if the list is empty
 *
 * @includeFunctionBody ../../test/ListTest.kt lastIndex
 */
val <T> List<T>.lastIndex : Int
    get() = this.size - 1

/**
 * Returns the first item in the list
 *
 * @includeFunctionBody ../../test/ListTest.kt head
 */
val <T> List<T>.head : T?
    get() = this.get(0)

/**
 * Returns all elements in this collection apart from the first one
 *
 * @includeFunctionBody ../../test/ListTest.kt tail
 */
val <T> List<T>.tail : List<T>
    get() {
        return drop(1)
    }
