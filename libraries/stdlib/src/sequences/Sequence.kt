package kotlin.sequences

/**
 * A sequence of items of type *T* that are lazily produced by the given closure
 *
 * @param next a closure yielding the next element in this sequence
 *
 * @author [Franck Rasolo](http://www.linkedin.com/in/franckrasolo)
 * @since 1.0
 */
class Sequence<in T>(val next: () -> T?) : Iterable<T> {

  override fun iterator(): Iterator<T> = YieldingIterator { (next)() }

  /**
   * Retains only elements that satisfy the given predicate
   *
   * @param predicate the predicate evaluated against objects of type *T*
   */
  fun filter(predicate: (T) -> Boolean): Sequence<T> {
    val iterator = iterator()

    fun next(): T? {
      while (iterator.hasNext) {
        val item = iterator.next()
        if ((predicate)(item)) return item
      }
      return null
    }

    return Sequence<T> { next() }
  }

  /**
   * Reduces this sequence using the binary operator, from left to right.
   * Only finite sequences can be reduced.
   *
   * @param seed an initial value, typically the left-identity of the operator
   * @param operator a binary operator
   */
  fun fold(seed: T, operator: (T, T) -> T): T {
    var result = seed
    for (item in this) result = (operator)(result, item)
    return result
  }

  /**
   * Produces a sequence obtained by applying *transform* to each element of this sequence
   *
   * @param transform the function transforming an object of type *T* into an object of type *R*
   */
  fun <in R> map(transform: (T) -> R): Sequence<R> {
    val iterator = iterator()
    fun next(): R? = if (iterator.hasNext) (transform)(iterator.next()) else null
    return Sequence<R> { next() }
  }

  /**
   * Extracts the prefix of this sequence as a finite sequence of length *n*
   *
   * @param n the number of elements of the extracted sequence
   */
  fun take(n: Int): Sequence<T> {
    fun countTo(n: Int): (T) -> Boolean {
      var count = 0
      return { ++count; count <= n }
    }
    return takeWhile(countTo(n))
  }

  /**
   * Extracts the longest prefix, possibly empty, of this sequence where each element satisfies *predicate*
   *
   * @param predicate the predicate evaluated against objects of type *T*
   */
  fun takeWhile(predicate: (T) -> Boolean): Sequence<T> {
    val iterator = iterator()

    fun next(): T? {
      if (iterator.hasNext) {
        val item = iterator.next()
        if ((predicate)(item)) return item
      }
      return null
    }

    return Sequence<T> { next() }
  }
}

private class YieldingIterator<T>(val yield: () -> T?) : Iterator<T> {
  var current : T? = (yield)()

  override val hasNext: Boolean
  get() = current != null

  override fun next(): T {
    val next = current
    if (next != null) {
      current = (yield)()
      return next
    }
    throw java.util.NoSuchElementException()
  }
}
