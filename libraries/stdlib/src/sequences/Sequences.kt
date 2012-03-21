package kotlin.sequences

/** Creates a sequence from the given list of *elements* */
inline fun <T> sequence(vararg elements: T): Sequence<T> = asSequence(elements.iterator())

/** Creates a sequence of elements lazily obtained from this Kotlin [[jet.Iterable]] */
inline fun <T> Iterable<T>.asSequence(): Sequence<T> = asSequence(iterator())

/** Creates a sequence of elements lazily obtained from this Java [[java.lang.Iterable]] */
inline fun <T> java.lang.Iterable<T>.asSequence(): Sequence<T> = asSequence(iterator().sure() as java.util.Iterator<T>)

private fun <T> asSequence(iterator: Iterator<T>) = Sequence<T> { if (iterator.hasNext) iterator.next() else null }
private fun <T> asSequence(iterator: java.util.Iterator<T>) = Sequence<T> { if (iterator.hasNext()) iterator.next() else null }

/**
 * Produces the [cartesian square](http://en.wikipedia.org/wiki/Cartesian_product#Cartesian_square_and_Cartesian_power)
 * as sequence of order pairs of elements lazily obtained from this range of elements
 */
fun <T> Iterable<T>.cartesianSquare(): Sequence<#(T, T)> {
  val first = iterator(); var second = iterator(); var a: T? = null

  fun nextPair(): #(T, T)? {
    if (a == null && first.hasNext) a = first.next()
    if (second.hasNext) return #(a.sure(), second.next())
    if (first.hasNext) {
      a = first.next(); second = iterator()
      return #(a.sure(), second.next())
    }
    return null
  }

  return Sequence<#(T, T)> { nextPair() }
}

/**
 * Partitions this string into groups of fixed size strings
 *
 * @param size the number of characters per group
 *
 * @return a sequence of strings of size *size*, except the last will be truncated if the elements do not divide evenly
 */
fun String.grouped(size: Int, iterator: CharIterator = iterator()): Sequence<String> {
  fun nextGroup(): String? {
    if (iterator.hasNext) {
      val window = StringBuilder()
      for (i in 1..size) if (iterator.hasNext) window.append(iterator.next())
      return window.toString()
    }
    return null
  }

  return Sequence<String> { nextGroup() }
}

/**
 * Groups elements in fixed size blocks by passing a *sliding window* over them, as opposed to partitioning them, as is done in `String.grouped`
 *
 * @param size the number of characters per group
 *
 * @return a sequence of strings of size *size*, except the last and the only element will be truncated if there are fewer characters than *size*
 */
fun String.sliding(size: Int, iterator: CharIterator = iterator()): Sequence<String> {
  val window = StringBuilder()

  fun nextWindow(): String? {
    if (window.length() == 0) {
      for (i in 1..size) if (iterator.hasNext) window.append(iterator.next())
      return window.toString()
    }
    return if (iterator.hasNext) window.deleteCharAt(0)?.append(iterator.next()).toString() else null
  }

  return Sequence<String> { nextWindow() }
}
