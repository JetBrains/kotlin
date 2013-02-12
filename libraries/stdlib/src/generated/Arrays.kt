package kotlin

import java.util.*

/**
 * Returns *true* if all elements match the given *predicate*
 */
public inline fun <T> Array<out T>.all(predicate: (T) -> Boolean) : Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns *true* if any elements match the given *predicate*
 */
public inline fun <T> Array<out T>.any(predicate: (T) -> Boolean) : Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of elements which match the given *predicate*
 */
public inline fun <T> Array<out T>.count(predicate: (T) -> Boolean) : Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the first element which matches the given *predicate* or *null* if none matched
 */
public inline fun <T:Any> Array<out T>.find(predicate: (T) -> Boolean) : T? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns a list containing all elements which match the given *predicate*
 */
public inline fun <T> Array<out T>.filter(predicate: (T) -> Boolean) : List<T> {
    return filterTo(ArrayList<T>(), predicate)
}

/**
 * Filters all elements which match the given predicate into the given list
 */
public inline fun <T, C: MutableCollection<in T>> Array<out T>.filterTo(result: C, predicate: (T) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element)
    return result
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun <T> Array<out T>.filterNot(predicate: (T) -> Boolean) : List<T> {
    return filterNotTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun <T, C: MutableCollection<in T>> Array<out T>.filterNotTo(result: C, predicate: (T) -> Boolean) : C {
    for (element in this) if (!predicate(element)) result.add(element)
    return result
}

/**
 * Returns a list containing all the non-*null* elements
 */
public inline fun <T:Any> Array<out T?>.filterNotNull() : List<T> {
    return filterNotNullTo<T, ArrayList<T>>(ArrayList<T>())
}

/**
 * Filters all non-*null* elements into the given list
 */
public inline fun <T:Any, C: MutableCollection<in T>> Array<out T?>.filterNotNullTo(result: C) : C {
    for (element in this) if (element != null) result.add(element)
    return result
}

/**
 * Partitions this collection into a pair of collections
 */
public inline fun <T> Array<out T>.partition(predicate: (T) -> Boolean) : Pair<List<T>, List<T>> {
    val first = ArrayList<T>()
    val second = ArrayList<T>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Returns a new List containing the results of applying the given *transform* function to each element in this collection
 */
public inline fun <T, R> Array<out T>.map(transform : (T) -> R) : List<R> {
    return mapTo(ArrayList<R>(), transform)
}

/**
 * Transforms each element of this collection with the given *transform* function and
 * adds each return value to the given *results* collection
 */
public inline fun <T, R, C: MutableCollection<in R>> Array<out T>.mapTo(result: C, transform : (T) -> R) : C {
    for (item in this)
        result.add(transform(item))
    return result
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single list
 */
public inline fun <T, R> Array<out T>.flatMap(transform: (T)-> Iterable<R>) : List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 */
public inline fun <T, R, C: MutableCollection<in R>> Array<out T>.flatMapTo(result: C, transform: (T) -> Iterable<R>) : C {
    for (element in this) {
        val list = transform(element)
        for (r in list) result.add(r)
    }
    return result
}

/**
 * Performs the given *operation* on each element
 */
public inline fun <T> Array<out T>.forEach(operation: (T) -> Unit) : Unit {
    for (element in this) operation(element)
}

/**
 * Folds all elements from from left to right with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <T, R> Array<out T>.fold(initial: R, operation: (R, T) -> R) : R {
    var answer = initial
    for (element in this) answer = operation(answer, element)
    return answer
}

/**
 * Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <T, R> Array<out T>.foldRight(initial: R, operation: (T, R) -> R) : R {
    var r = initial
    var index = size - 1
    
    while (index >= 0) {
        r = operation(get(index--), r)
    }
    
    return r
}

/**
 * Applies binary operation to all elements of iterable, going from left to right.
 * Similar to fold function, but uses the first element as initial value
 */
public inline fun <T> Array<out T>.reduce(operation: (T, T) -> T) : T {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw UnsupportedOperationException("Empty iterable can't be reduced")
    }
    
    var result: T = iterator.next() //compiler doesn't understand that result will initialized anyway
    while (iterator.hasNext()) {
        result = operation(result, iterator.next())
    }
    
    return result
}

/**
 * Applies binary operation to all elements of iterable, going from right to left.
 * Similar to foldRight function, but uses the last element as initial value
 */
public inline fun <T> Array<out T>.reduceRight(operation: (T, T) -> T) : T {
    var index = size - 1
    if (index < 0) {
        throw UnsupportedOperationException("Empty iterable can't be reduced")
    }
    
    var r = get(index--)
    while (index >= 0) {
        r = operation(get(index--), r)
    }
    
    return r
}

/**
 * Groups the elements in the collection into a new [[Map]] using the supplied *toKey* function to calculate the key to group the elements by
 */
public inline fun <T, K> Array<out T>.groupBy(toKey: (T) -> K) : Map<K, List<T>> {
    return groupByTo(HashMap<K, MutableList<T>>(), toKey)
}

public inline fun <T, K> Array<out T>.groupByTo(result: MutableMap<K, MutableList<T>>, toKey: (T) -> K) : Map<K, MutableList<T>> {
    for (element in this) {
        val key = toKey(element)
        val list = result.getOrPut(key) { ArrayList<T>() }
        list.add(element)
    }
    return result
}

/**
 * Returns a list containing everything but the first *n* elements
 */
public inline fun <T> Array<out T>.drop(n: Int) : List<T> {
    return dropWhile(countTo(n))
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun <T> Array<out T>.dropWhile(predicate: (T) -> Boolean) : List<T> {
    return dropWhileTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun <T, L: MutableList<in T>> Array<out T>.dropWhileTo(result: L, predicate: (T) -> Boolean) : L {
    var start = true
    for (element in this) {
        if (start && predicate(element)) {
            // ignore
        } else {
            start = false
            result.add(element)
        }
    }
    return result
}

/**
 * Returns a list containing the first *n* elements
 */
public inline fun <T> Array<out T>.take(n: Int) : List<T> {
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun <T> Array<out T>.takeWhile(predicate: (T) -> Boolean) : List<T> {
    return takeWhileTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun <T, C: MutableCollection<in T>> Array<out T>.takeWhileTo(result: C, predicate: (T) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element) else break
    return result
}

/**
 * Copies all elements into the given collection
 */
public inline fun <T, C: MutableCollection<in T>> Array<out T>.toCollection(result: C) : C {
    for (element in this) result.add(element)
    return result
}

/**
 * Reverses the order the elements into a list
 */
public inline fun <T> Array<out T>.reverse() : List<T> {
    val list = toCollection(ArrayList<T>())
    Collections.reverse(list)
    return list
}

/**
 * Copies all elements into a [[LinkedList]]
 */
public inline fun <T> Array<out T>.toLinkedList() : LinkedList<T> {
    return toCollection(LinkedList<T>())
}

/**
 * Copies all elements into a [[List]]
 */
public inline fun <T> Array<out T>.toList() : List<T> {
    return toCollection(ArrayList<T>())
}

/**
 * Copies all elements into a [[Set]]
 */
public inline fun <T> Array<out T>.toSet() : Set<T> {
    return toCollection(LinkedHashSet<T>())
}

/**
 * Copies all elements into a [[SortedSet]]
 */
public inline fun <T> Array<out T>.toSortedSet() : SortedSet<T> {
    return toCollection(TreeSet<T>())
}

/**
 * Returns a original Iterable containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements
 */
public inline fun <T:Any> Array<out T?>.requireNoNulls() : Array<out T> {
    for (element in this) {
        if (element == null) {
            throw IllegalArgumentException("null element found in $this")
        }
    }
    return this as Array<out T>
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the given element at the end
 */
public inline fun <T> Array<out T>.plus(element: T) : List<T> {
    val answer = ArrayList<T>()
    toCollection(answer)
    answer.add(element)
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following iterator
 */
public inline fun <T> Array<out T>.plus(iterator: Iterator<T>) : List<T> {
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
public inline fun <T> Array<out T>.plus(collection: Iterable<T>) : List<T> {
    return plus(collection.iterator())
}

/**
 * Returns an iterator of Pairs(index, data)
 */
public inline fun <T> Array<out T>.withIndices() : Iterator<Pair<Int, T>> {
    return IndexIterator(iterator())
}

/**
 * Copies all elements into a [[List]] and sorts it by value of compare_function(element)
 * E.g. arrayList("two" to 2, "one" to 1).sortBy({it._2}) returns list sorted by second element of tuple
 */
public inline fun <T, R: Comparable<R>> Array<out T>.sortBy(f: (T) -> R) : List<T> {
    val sortedList = toCollection(ArrayList<T>())
    val sortBy: Comparator<T> = comparator<T> {(x: T, y: T) ->
        val xr = f(x)
        val yr = f(y)
        xr.compareTo(yr)
    }
    java.util.Collections.sort(sortedList, sortBy)
    return sortedList
}

/**
 * Appends the string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied
 * If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
 * a special *truncated* separator (which defaults to "..."
 */
public inline fun <T> Array<out T>.appendString(buffer: Appendable, separator: String = ", ", prefix: String ="", postfix: String = "", limit: Int = -1, truncated: String = "...") : Unit {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            val text = if (element == null) "null" else element.toString()
            buffer.append(text)
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
}

/**
 * Creates a string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied.
 * If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
 * a special *truncated* separator (which defaults to "..."
 */
public inline fun <T> Array<out T>.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "...") : String {
    val buffer = StringBuilder()
    appendString(buffer, separator, prefix, postfix, limit, truncated)
    return buffer.toString()
}

