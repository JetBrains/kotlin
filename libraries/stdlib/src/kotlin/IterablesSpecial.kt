package kotlin
// Number of extension function for java.lang.Iterable that shouldn't participate in auto generation

import java.util.AbstractList
import java.util.Comparator
import java.util.ArrayList

/**
 * Count the number of elements in collection.
 *
 * If base collection implements [[Collection]] interface method [[Collection.size()]] will be used.
 * Otherwise, this method determines the count by iterating through the all items.
 */
public fun <T> Iterable<T>.count() : Int {
  if (this is Collection<T>) {
    return this.size()
  }

  var number : Int = 0
  for (elem in this) {
    ++number
  }
  return number
}

private fun <T> countTo(n: Int): (T) -> Boolean {
  var count = 0
  return { ++count; count <= n }
}


/**
 * Get the first element in the collection.
 *
 * Will throw an exception if there are no elements
 */
public inline fun <T> Iterable<T>.first() : T {
  if (this is List<T>) {
    return this.first()
  }

  return this.iterator().next()
}

/**
 * Checks if collection contains given item.
 *
 * Method checks equality of the objects with T.equals method.
 * If collection implements [[java.util.AbstractCollection]] an overridden implementation of the contains
 * method will be used.
 */
public fun <T> Iterable<T>.containsItem(item : T) : Boolean {
  if (this is java.util.AbstractCollection<T>) {
    return this.contains(item);
  }

  for (var elem in this) {
    if (elem == item) {
      return true
    }
  }

  return false
}


public inline fun <T: Comparable<T>> MutableIterable<T>.sort() : List<T> {
    val list = toCollection(ArrayList<T>())
    java.util.Collections.sort(list)
    return list
}

public inline fun <T> Iterable<T>.sort(comparator: java.util.Comparator<T>) : List<T> {
    val list = toCollection(ArrayList<T>())
    java.util.Collections.sort(list, comparator)
    return list
}
