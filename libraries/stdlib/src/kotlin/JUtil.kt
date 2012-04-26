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
public inline fun arrayList<T>(vararg values: T) : ArrayList<T> = values.toCollection(ArrayList<T>(values.size))

/** Returns a new LinkedList with a variable number of initial elements */
public inline fun linkedList<T>(vararg values: T) : LinkedList<T>  = values.toCollection(LinkedList<T>())

/** Returns a new HashSet with a variable number of initial elements */
public inline fun hashSet<T>(vararg values: T) : HashSet<T> = values.toCollection(HashSet<T>(values.size))

/**
 * Returns a new [[SortedSet]] with the initial elements
 */
public inline fun sortedSet<T>(vararg values: T) : TreeSet<T> = values.toCollection(TreeSet<T>())

/**
 * Returns a new [[SortedSet]] with the given *comparator* and the initial elements
 */
public inline fun sortedSet<T>(comparator: Comparator<T>, vararg values: T) : TreeSet<T> = values.toCollection(TreeSet<T>(comparator))

/**
 * Returns a new [[HashMap]] populated with the given tuple values where the first value in each tuple
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/MapTest.kt createUsingTuples
 */
public inline fun <K,V> hashMap(vararg values: #(K,V)): HashMap<K,V> {
    val answer = HashMap<K,V>(values.size)
    /**
        TODO replace by this simpler call when we can pass vararg values into other methods
        answer.putAll(values)
    */
    for (v in values) {
        answer.put(v._1, v._2)
    }
    return answer
}

/**
 * Returns a new [[SortedMap]] populated with the given tuple values where the first value in each tuple
 * is the key and the second value is the value
 *
 * @includeFunctionBody ../../test/MapTest.kt createSortedMap
 */
public inline fun <K,V> sortedMap(vararg values: #(K,V)): SortedMap<K,V> {
    val answer = TreeMap<K,V>()
    /**
        TODO replace by this simpler call when we can pass vararg values into other methods
        answer.putAll(values)
    */
    for (v in values) {
        answer.put(v._1, v._2)
    }
    return answer
}

/**
 * Returns a new [[LinkedHashMap]] populated with the given tuple values where the first value in each tuple
 * is the key and the second value is the value. This map preserves insertion order so iterating through
 * the map's entries will be in the same order
 *
 * @includeFunctionBody ../../test/MapTest.kt createLinkedMap
 */
public inline fun <K,V> linkedMap(vararg values: #(K,V)): LinkedHashMap<K,V> {
    val answer = LinkedHashMap<K,V>(values.size)
    /**
        TODO replace by this simpler call when we can pass vararg values into other methods
        answer.putAll(values)
    */
    for (v in values) {
        answer.put(v._1, v._2)
    }
    return answer
}


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
    = if (this != null) this else Collections.EMPTY_LIST as Collection<T>


/** TODO these functions don't work when they generate the Array<T> versions when they are in JLIterables */
public inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList() : List<T> = toList().sort()

public inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.toSortedList(comparator: java.util.Comparator<T>) : List<T> = toList().sort(comparator)


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

val <T> List<T>.lastIndex : Int
    get() = this.size - 1
