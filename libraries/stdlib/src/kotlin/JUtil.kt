package kotlin

import java.util.*

/** Returns the size of the collection */
public val Collection<*>.size : Int
    get() = size()

/** Returns true if this collection is empty */
public val Collection<*>.empty : Boolean
    get() = isEmpty()

public val Collection<*>.indices : IntRange
    get() = 0..size-1

public val Int.indices: IntRange
    get() = 0..this-1

/** Returns true if the collection is not empty */
public fun <T> Collection<T>.isNotEmpty() : Boolean = !this.isEmpty()

/** Returns true if this collection is not empty */
val Collection<*>.notEmpty : Boolean
    get() = isNotEmpty()

/** Returns the Collection if its not null otherwise it returns the empty list */
public fun <T> Collection<T>?.orEmpty() : Collection<T> = this ?: Collections.emptyList<T>()


/** TODO these functions don't work when they generate the Array<T> versions when they are in JLIterables */
public fun <T: Comparable<T>> Iterable<T>.toSortedList() : List<T> = toCollection(ArrayList<T>()).sort()

public fun <T: Comparable<T>> Iterable<T>.toSortedList(comparator: java.util.Comparator<T>) : List<T> = toList().sort(comparator)


// List APIs

/** Returns the List if its not null otherwise returns the empty list */
public fun <T> List<T>?.orEmpty() : List<T> = this ?: Collections.emptyList<T>()

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
    get() = if (this.isNotEmpty()) this.get(0) else null

/**
 * Returns all elements in this collection apart from the first one
 *
 * @includeFunctionBody ../../test/ListTest.kt tail
 */
val <T> List<T>.tail : List<T>
    get() {
        return this.drop(1)
    }
