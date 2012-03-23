package kotlin.sequences

import com.sun.tools.javac.resources.javac

/** Creates a sequence without elements of type *T* */
inline fun <T> empty(): Sequence<T> = Sequence<T>

/** Creates a sequence from the given list of *elements* */
inline fun <T> sequence(vararg elements: T): Sequence<T> {
  var iterator: Iterator<T>

  fun reset() { iterator = elements.iterator() }
  fun next(): T? = if (iterator.hasNext) iterator.next() else null

  return Sequence<T>({ reset() }, { next() })
}

/** Creates a sequence of elements lazily obtained from this Kotlin [[jet.Iterable]] */
inline fun <T> Iterable<T>.asSequence(): Sequence<T> {
  var iterator: Iterator<T>

  fun reset() { iterator = iterator() }
  fun next(): T? = if (iterator.hasNext) iterator.next() else null

  return Sequence<T>({ reset() }, { next() })
}

/** Creates a sequence of elements lazily obtained from this Java [[java.lang.Iterable]] */
inline fun <T> java.lang.Iterable<T>.asSequence(): Sequence<T> {
  var iterator: java.util.Iterator<T>

  fun reset() { iterator = iterator().sure() }
  fun next(): T? = if (iterator.hasNext()) iterator.next() else null

  return Sequence<T>({ reset() }, { next() })
}

/**
 * Produces the [cartesian product](http://en.wikipedia.org/wiki/Cartesian_product#n-ary_product) as a sequence of ordered pairs of elements lazily obtained
 * from two [[Iterable]] instances
 */
fun <T> Iterable<T>.times(other: Iterable<T>): Sequence<#(T, T)> {
  var first: Iterator<T>; var second: Iterator<T>; var a: T?

  fun reset() { first = iterator(); second = other.iterator(); a = null }

  fun nextPair(): #(T, T)? {
    if (a == null && first.hasNext) a = first.next()
    if (second.hasNext) return #(a.sure(), second.next())
    if (first.hasNext) {
      a = first.next(); second = other.iterator()
      return #(a.sure(), second.next())
    }
    return null
  }

  return Sequence<#(T, T)>({ reset() }, { nextPair() })
}

/**
 * Partitions this string into groups of fixed size strings
 *
 * @param size the number of characters per group
 *
 * @return a sequence of strings of size *size*, except the last will be truncated if the elements do not divide evenly
 */
fun String.grouped(size: Int): Sequence<String> {
  var iterator: CharIterator

  fun reset() { iterator = iterator() }
  fun nextGroup(): String? {
    if (iterator.hasNext) {
      val window = StringBuilder()
      for (i in 1..size) if (iterator.hasNext) window.append(iterator.next())
      return window.toString()
    }
    return null
  }

  return Sequence<String>({ reset() }, { nextGroup() })
}

/**
 * Groups elements in fixed size blocks by passing a *sliding window* over them, as opposed to partitioning them, as is done in `String.grouped`
 *
 * @param size the number of characters per group
 *
 * @return a sequence of strings of size *size*, except the last and the only element will be truncated if there are fewer characters than *size*
 */
fun String.sliding(size: Int): Sequence<String> {
  var iterator: CharIterator; var window: StringBuilder

  fun reset() { iterator = iterator(); window = StringBuilder() }

  fun nextWindow(): String? {
    if (window.length() == 0) {
      for (i in 1..size) if (iterator.hasNext) window.append(iterator.next())
      return window.toString()
    }
    return if (iterator.hasNext) window.deleteCharAt(0)?.append(iterator.next()).toString() else null
  }

  return Sequence<String>({ reset() }, { nextWindow() })
}
