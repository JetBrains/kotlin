@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

import java.util.*

/**
 * Creates an [Iterator] for an [Enumeration], allowing to use it in `for` loops.
 */
@kotlin.jvm.JvmVersion
public operator fun <T> Enumeration<T>.iterator(): Iterator<T> = object : Iterator<T> {
    override fun hasNext(): Boolean = hasMoreElements()

    public override fun next(): T = nextElement()
}

/**
 * Returns the given iterator itself. This allows to use an instance of iterator in a `for` loop.
 */
@kotlin.internal.InlineOnly
public inline operator fun <T> Iterator<T>.iterator(): Iterator<T> = this

/**
 * Returns an [Iterator] wrapping each value produced by this [Iterator] with the [IndexedValue],
 * containing value and it's index.
 */
public fun <T> Iterator<T>.withIndex(): Iterator<IndexedValue<T>> = IndexingIterator(this)

/**
 * Performs the given [operation] on each element of this [Iterator].
 */
public inline fun <T> Iterator<T>.forEach(operation: (T) -> Unit) : Unit {
    for (element in this) operation(element)
}

/**
 * Iterator transforming original `iterator` into iterator of [IndexedValue], counting index from zero.
 */
internal class IndexingIterator<out T>(private val iterator: Iterator<T>) : Iterator<IndexedValue<T>> {
    private var index = 0
    final override fun hasNext(): Boolean = iterator.hasNext()
    final override fun next(): IndexedValue<T> = IndexedValue(index++, iterator.next())
}
