package kotlin2

import kotlin.internal.ArrayIterator

/**
 * Annotates a class used to implement extension functions
 */
annotation class extension() 

/**
 * Defines the extension functions on a List<T>
 */
extension open class ListExtensions<T>(private val that: List<T>): CollectionExtensions<T>(that), ListLike<T> {
    public override fun get(index: Int): T {
        return that.get(index)!!
    }
}

/**
 * Defines the extension functions on a Collection<T>
 */
extension open class CollectionExtensions<T>(private val that: Collection<T>): CollectionLike<T> {
    public override fun iterator(): Iterator<T> {
        // TODO adapt java.util.Iterator<T> to Iterator<T>
        throw UnsupportedOperationException()
    }
    public override fun size(): Int {
        return that.size()
    }
    public override fun contains(item: T): Boolean {
        return that.contains(item)
    }
}

/**
 * Defines the extension functions on a Collection<T>
 */
extension open class IteratorExtensions<T>(private val that: Iterator<T>): LazyTraversable<T> {
    public override fun iterator(): Iterator<T> {
        // TODO adapt java.util.Iterator<T> to Iterator<T>
        throw UnsupportedOperationException()
    }
}

/**
 * Defines the extension functions on an Array<T>
 */
extension open class ArrayExtensions<T>(private val that: Array<T>): ListLike<T> {
    public override fun get(index: Int): T {
        return that[index]
    }

    public override fun contains(item: T): Boolean {
        throw UnsupportedOperationException()
    }

    public override fun iterator(): Iterator<T> {
        // TODO
        //return ArrayIterator(that)
        throw UnsupportedOperationException()
    }

    public override fun size(): Int {
        return that.size
    }
}
