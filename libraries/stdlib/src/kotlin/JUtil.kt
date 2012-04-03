package kotlin

import java.util.*
import java.util.regex.Pattern

/** Returns the size of the collection */
val Collection<*>.size : Int
    get() = size()

/** Returns true if this collection is empty */
val Collection<*>.empty : Boolean
    get() = isEmpty()

/** Returns a new ArrayList with a variable number of initial elements */
public inline fun arrayList<T>(vararg values: T) : ArrayList<T> = values.to(ArrayList<T>(values.size))

/** Returns a new LinkedList with a variable number of initial elements */
public inline fun linkedList<T>(vararg values: T) : LinkedList<T>  = values.to(LinkedList<T>())

/** Returns a new HashSet with a variable number of initial elements */
public inline fun hashSet<T>(vararg values: T) : HashSet<T> = values.to(HashSet<T>(values.size))

/** Returns a new SortedSet with a variable number of initial elements */
public inline fun sortedSet<T>(vararg values: T) : TreeSet<T> = values.to(TreeSet<T>())

public inline fun <K,V> hashMap(): HashMap<K,V> = HashMap<K,V>()

public inline fun <K,V> sortedMap(): SortedMap<K,V> = TreeMap<K,V>()


val Collection<*>.indices : IntRange
    get() = 0..size-1

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
    = if (this != null) this else Collections.EMPTY_LIST as Collection<T>


/** TODO these functions don't work when they generate the Array<T> versions when they are in JLIterables */
public inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList() : List<T> = toList().sort()

public inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList(comparator: java.util.Comparator<T>) : List<T> = toList().sort(comparator)


/**
 * Creates a new [[List]] with the element added at the end
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plus
 */
public inline fun <in T> java.util.Collection<T>.plus(element: T): List<in T> {
    val list = to(ArrayList<T>())
    list.add(element)
    return list
}

/**
 * Adds the element to this collection
 *
 * @includeFunctionBody ../../test/CollectionTest.kt plusAssign
 */
public inline fun <in T, C: java.util.Collection<in T>> C.plusAssign(element: T): C {
    add(element)
    return this
}

/**
 * Creates a new [[List]] with the element added at the end
 */
/*
public inline fun <in T> java.util.Collection<T>.plus(elements: java.lang.Iterable<T>): List<T> = toList().plusAssign(elements)
*/

/**
 * Adds all the elements to this collection
 */
/*
public inline fun <in T, C: Collection<in T>> C.plusAssign(elements: java.lang.Iterable<T>): C {
    addAll(elements.toCollection())
    return this
}
*/


// List APIs

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
    = if (this != null) this else Collections.EMPTY_LIST as java.util.List<T>

/** Returns the Set if its not null otherwise returns the empty set */
public inline fun <T> java.util.Set<T>?.orEmpty() : java.util.Set<T>
    = if (this != null) this else Collections.EMPTY_SET as java.util.Set<T>

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
