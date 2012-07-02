package kotlin
// Number of extension function for java.lang.Iterable that shouldn't participate in auto generation

import java.util.Collection
import java.util.List
import java.util.AbstractList
import java.util.Iterator
import java.util.Comparator

/**
 * Count the number of elements in collection.
 *
 * If base collection implements [[Collection]] interface method [[Collection.size()]] will be used.
 * Otherwise, this method determines the count by iterating through the all items.
 */
public fun <T> java.lang.Iterable<T>.count() : Int {
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
// TODO: Specify type of the exception
public inline fun <T> java.lang.Iterable<T>.first() : T {
  if (this is AbstractList<T>) {
    return this.get(0)
  }

  return this.iterator().sure().next()
}

/**
 * Get the last element in the collection.
 *
 * If base collection implements [[List]] interface, the combination of size() and get()
 * methods will be used for getting last element. Otherwise, this method determines the
 * last item by iterating through the all items.
 *
 * Will throw an exception if there are no elements.
 *
 * @includeFunctionBody ../../test/CollectionTest.kt last
 */
// TODO: Specify type of the exception
public fun <T> java.lang.Iterable<T>.last() : T {
  if (this is List<T>) {
    return this.get(this.size() - 1);
  }

  val iterator = this.iterator().sure()
  var last : T = iterator.next()

  while (iterator.hasNext()) {
    last = iterator.next()
  }

  return last;
}

/**
 * Checks if collection contains given item.
 *
 * Method checks equality of the objects with T.equals method.
 * If collection implements [[java.util.AbstractCollection]] an overridden implementation of the contains
 * method will be used.
 */
public fun <T> java.lang.Iterable<T>.contains(item : T) : Boolean {
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

/**
 * Convert collection of arbitrary elements to collection of tuples of the index and the element
 *
 * @includeFunctionBody ../../test/ListTest.kt withIndices
 */
public fun <T> java.lang.Iterable<T>.withIndices() : java.lang.Iterable<#(Int, T)> {
    return object : java.lang.Iterable<#(Int, T)> {
        public override fun iterator(): java.util.Iterator<#(Int, T)> {
            return NumberedIterator<T>(this@withIndices.iterator()!!)
        }
    }
}

public inline fun <in T: java.lang.Comparable<T>> java.lang.Iterable<T>.sort() : List<T> {
    val list = toList()
    java.util.Collections.sort(list)
    return list
}

public inline fun <in T> java.lang.Iterable<T>.sort(comparator: java.util.Comparator<T>) : List<T> {
    val list = toList()
    java.util.Collections.sort(list, comparator)
    return list
}

private class NumberedIterator<TT>(private val sourceIterator : java.util.Iterator<TT>) : java.util.Iterator<#(Int, TT)> {
    private var nextIndex = 0

    public override fun remove() {
        try {
            sourceIterator.remove()
            nextIndex--;
        } catch (e : UnsupportedOperationException) {
            throw e
        }
    }

    public override fun hasNext(): Boolean {
        return sourceIterator.hasNext();
    }

    public override fun next(): #(Int, TT) {
        val result = #(nextIndex, sourceIterator.next())
        nextIndex++
        return result
    }
}