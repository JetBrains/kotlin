package kotlin

import java.util.Enumeration

/**
 * Helper to make java.util.Enumeration usable in for
 */
public fun <T> Enumeration<T>.iterator(): Iterator<T> = object : Iterator<T> {
    override fun hasNext(): Boolean = hasMoreElements()

    public override fun next(): T = nextElement()
}

/**
 * Returns the given iterator itself. This allows to use an instance of iterator in a ranged for-loop
 */
public fun <T> Iterator<T>.iterator(): Iterator<T> = this

/**
 * Data class representing a value with an index
 */
public data class IndexedValue<out T>(public val index: Int, public val value: T)

public class IndexingIterable<out T>(private val iteratorFactory: () -> Iterator<T>) : Iterable<IndexedValue<T>> {
    override fun iterator(): Iterator<IndexedValue<T>> = IndexingIterator(iteratorFactory())
}

/**
 * Iterator transforming original *iterator* into iterator of [IndexedValue], counting index from zero.
 */
public class IndexingIterator<out T>(private val iterator: Iterator<T>) : Iterator<IndexedValue<T>> {
    private var index = 0
    final override fun hasNext(): Boolean = iterator.hasNext()
    final override fun next(): IndexedValue<T> = IndexedValue(index++, iterator.next())
}
