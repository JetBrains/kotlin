package kotlin

/**
 * Data class representing a value from a collection or sequence, along with its index in that collection or sequence.
 *
 * @property value the underlying value.
 * @property index the index of the value in the collection or sequence.
 */
public data class IndexedValue<out T>(public val index: Int, public val value: T)

/**
 * A wrapper over another [Iterable] (or any other object that can produce an [Iterator]) that returns
 * an indexing iterator.
 */
@Deprecated("This implementation class will become internal soon. Use Iterable<IndexedValue<T>> instead.", ReplaceWith("Iterable<IndexedValue<T>>"))
public class IndexingIterable<out T>(private val iteratorFactory: () -> Iterator<T>) : Iterable<IndexedValue<T>> {
    override fun iterator(): Iterator<IndexedValue<T>> = IndexingIterator(iteratorFactory())
}

/**
 * Iterator transforming original `iterator` into iterator of [IndexedValue], counting index from zero.
 */
@Deprecated("This implementation class will become internal soon. Use Iterator<IndexedValue<T>> instead.", ReplaceWith("Iterator<IndexedValue<T>>"))
public class IndexingIterator<out T>
@Deprecated("This implementation class will become internal soon. Use iterator.withIndex() instead.", ReplaceWith("iterator.withIndex()"))
constructor(private val iterator: Iterator<T>) : Iterator<IndexedValue<T>> {
    private var index = 0
    final override fun hasNext(): Boolean = iterator.hasNext()
    final override fun next(): IndexedValue<T> = IndexedValue(index++, iterator.next())
}