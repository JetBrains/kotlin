package kotlin

import java.util.*

/**
 * Returns a list containing all elements which match the given *predicate*
 */
public inline fun <T> Collection<T>.filter(predicate: (T) -> Boolean) : List<T> {
    return filterTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun <T> Collection<T>.filterNot(predicate: (T) -> Boolean) : List<T> {
    return filterNotTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing all the non-*null* elements
 */
public inline fun <T:Any> Collection<T?>.filterNotNull() : List<T> {
    return filterNotNullTo<T, ArrayList<T>>(ArrayList<T>())
}

/**
 * Returns a new List containing the results of applying the given *transform* function to each element in this collection
 */
public inline fun <T, R> Collection<T>.map(transform : (T) -> R) : List<R> {
    return mapTo(ArrayList<R>(), transform)
}

/**
 * Returns a list containing the first *n* elements
 */
public inline fun <T> Collection<T>.take(n: Int) : List<T> {
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun <T> Collection<T>.takeWhile(predicate: (T) -> Boolean) : List<T> {
    return takeWhileTo(ArrayList<T>(), predicate)
}

/**
 * Returns a original Iterable containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements
 */
public inline fun <T:Any> Collection<T?>.requireNoNulls() : Collection<T> {
    for (element in this) {
        if (element == null) {
            throw IllegalArgumentException("null element found in $this")
        }
    }
    return this as Collection<T>
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the given element at the end
 */
public inline fun <T> Collection<T>.plus(element: T) : List<T> {
    val answer = ArrayList<T>()
    toCollection(answer)
    answer.add(element)
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following iterator
 */
public inline fun <T> Collection<T>.plus(iterator: Iterator<T>) : List<T> {
    val answer = ArrayList<T>()
    toCollection(answer)
    for (element in iterator) {
        answer.add(element)
    }
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following collection
 */
public inline fun <T> Collection<T>.plus(collection: Iterable<T>) : List<T> {
    return plus(collection.iterator())
}

