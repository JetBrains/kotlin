package kotlin.sequences

/**
 * A sequence of elements of type *T* that are lazily produced by the given closure
 *
 * @param reset a closure initialising the iteration context
 * @param next a closure yielding the next element in this sequence
 *
 * @author [Franck Rasolo](http://www.linkedin.com/in/franckrasolo)
 * @since 1.0
 */
class Sequence<in T>(val reset: () -> Unit = {}, val next: () -> T? = { null }) : Iterable<T> {

  override fun iterator(): Iterator<T> { (reset)(); return YieldingIterator { (next)() } }

  fun equals(obj: Any?): Boolean = obj is Sequence<T> && iterator() == obj.iterator()

  fun toString(): String = join(separator = ", ", prefix = "[ ", postfix = " ]")

  /**
   * Creates a string from the first *n (limit)* elements in the sequence, separated by *separator* and using the given *prefix* and *postfix* if supplied
   */
  fun join(separator: String = " : ", prefix : String = "", postfix : String = "", limit: Int = 30): String {
    val iterator = iterator(); val builder = StringBuilder(); var count = 0
    builder.append(prefix)

    if (iterator.hasNext) builder.append(iterator.next())
    count++

    while (iterator.hasNext && count < limit) {
      count++
      builder.append(separator)?.append(iterator.next())
    }

    if (count == limit) builder.append(separator)?.append("...")
    builder.append(postfix)

    return builder.toString().sure()
  }

  /**
   * Retains only elements that satisfy the given predicate
   *
   * @param predicate the predicate evaluated against objects of type *T*
   */
  fun filter(predicate: (T) -> Boolean): Sequence<T> {
    var iterator: Iterator<T>

    fun reset() { iterator = iterator() }
    fun next(): T? {
      while (iterator.hasNext) {
        val item = iterator.next()
        if ((predicate)(item)) return item
      }
      return null
    }

    return Sequence<T>({ reset() }, { next() })
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
    var iterator: Iterator<T>

    fun reset() { iterator = iterator() }
    fun next(): R? = if (iterator.hasNext) (transform)(iterator.next()) else null

    return Sequence<R>({ reset() }, { next() })
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
    var iterator: Iterator<T>

    fun reset() { iterator = iterator() }
    fun next(): T? {
      if (iterator.hasNext) {
        val item = iterator.next()
        if ((predicate)(item)) return item
      }
      return null
    }

    return Sequence<T>({ reset() }, { next() })
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

  fun <T> equals(obj: Any?): Boolean = obj is YieldingIterator<T> && equal(this, obj as YieldingIterator<T>)

  private fun <T> equal(a: Iterator<T>, b: Iterator<T>): Boolean {
    while (a.hasNext && b.hasNext) {
      val elementOfA = a.next()
      val elementOfB = b.next()

      if (elementOfA == elementOfB) continue
      if (elementOfA == null || elementOfA != elementOfB) return false
    }
    return !a.hasNext && !b.hasNext
  }
}
