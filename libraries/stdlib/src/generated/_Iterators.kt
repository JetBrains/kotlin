package kotlin

import java.util.*

/**
 * Returns an iterator over elements which match the given *predicate*
 */
public inline fun <T> Iterator<T>.filter(predicate: (T) -> Boolean) : Iterator<T> {
    return FilterIterator<T>(this, predicate)
}

/**
 * Returns an iterator over elements which don't match the given *predicate*
 */
public inline fun <T> Iterator<T>.filterNot(predicate: (T) -> Boolean) : Iterator<T> {
    return filter {!predicate(it)}
}

/**
 * Returns an iterator over non-*null* elements
 */
public inline fun <T:Any> Iterator<T?>.filterNotNull() : Iterator<T> {
    return FilterNotNullIterator(this)
}

/**
 * Returns an iterator obtained by applying *transform*, a function transforming an object of type *T* into an object of type *R*
 */
public inline fun <T, R> Iterator<T>.map(transform : (T) -> R) : Iterator<R> {
    return MapIterator<T, R>(this, transform)
}

/**
 * Returns an iterator over the concatenated results of transforming each element to one or more values
 */
public inline fun <T, R> Iterator<T>.flatMap(transform: (T) -> Iterator<R>) : Iterator<R> {
    return FlatMapIterator<T, R>(this, transform)
}

/**
 * Returns a original Iterable containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements
 */
public inline fun <T:Any> Iterator<T?>.requireNoNulls() : Iterator<T> {
    return map<T?, T>{
        if (it == null) throw IllegalArgumentException("null element in iterator $this") else it
    }
}

/**
 * Returns an iterator restricted to the first *n* elements
 */
public inline fun <T> Iterator<T>.take(n: Int) : Iterator<T> {
    var count = n
    return takeWhile{ --count >= 0 }
}

/**
 * Returns an iterator restricted to the first elements that match the given *predicate*
 */
public inline fun <T> Iterator<T>.takeWhile(predicate: (T) -> Boolean) : Iterator<T> {
    return TakeWhileIterator<T>(this, predicate)
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the given element at the end
 */
public inline fun <T> Iterator<T>.plus(element: T) : Iterator<T> {
    return CompositeIterator<T>(this, SingleIterator(element))
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following iterator
 */
public inline fun <T> Iterator<T>.plus(iterator: Iterator<T>) : Iterator<T> {
    return CompositeIterator<T>(this, iterator)
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following collection
 */
public inline fun <T> Iterator<T>.plus(collection: Iterable<T>) : Iterator<T> {
    return plus(collection.iterator())
}

