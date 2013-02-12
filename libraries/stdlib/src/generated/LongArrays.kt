package kotlin

import java.util.*

/**
 * Returns *true* if all elements match the given *predicate*
 */
public inline fun LongArray.all(predicate: (Long) -> Boolean) : Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns *true* if any elements match the given *predicate*
 */
public inline fun LongArray.any(predicate: (Long) -> Boolean) : Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of elements which match the given *predicate*
 */
public inline fun LongArray.count(predicate: (Long) -> Boolean) : Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the first element which matches the given *predicate* or *null* if none matched
 */
public inline fun LongArray.find(predicate: (Long) -> Boolean) : Long? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns a list containing all elements which match the given *predicate*
 */
public inline fun LongArray.filter(predicate: (Long) -> Boolean) : List<Long> {
    return filterTo(ArrayList<Long>(), predicate)
}

/**
 * Filters all elements which match the given predicate into the given list
 */
public inline fun <C: MutableCollection<in Long>> LongArray.filterTo(result: C, predicate: (Long) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element)
    return result
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun LongArray.filterNot(predicate: (Long) -> Boolean) : List<Long> {
    return filterNotTo(ArrayList<Long>(), predicate)
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun <C: MutableCollection<in Long>> LongArray.filterNotTo(result: C, predicate: (Long) -> Boolean) : C {
    for (element in this) if (!predicate(element)) result.add(element)
    return result
}

/**
 * Partitions this collection into a pair of collections
 */
public inline fun LongArray.partition(predicate: (Long) -> Boolean) : Pair<List<Long>, List<Long>> {
    val first = ArrayList<Long>()
    val second = ArrayList<Long>()
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
public inline fun <R> LongArray.map(transform : (Long) -> R) : List<R> {
    return mapTo(ArrayList<R>(), transform)
}

/**
 * Transforms each element of this collection with the given *transform* function and
 * adds each return value to the given *results* collection
 */
public inline fun <R, C: MutableCollection<in R>> LongArray.mapTo(result: C, transform : (Long) -> R) : C {
    for (item in this)
        result.add(transform(item))
    return result
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single list
 */
public inline fun <R> LongArray.flatMap(transform: (Long)-> Iterable<R>) : List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 */
public inline fun <R, C: MutableCollection<in R>> LongArray.flatMapTo(result: C, transform: (Long) -> Iterable<R>) : C {
    for (element in this) {
        val list = transform(element)
        for (r in list) result.add(r)
    }
    return result
}

/**
 * Performs the given *operation* on each element
 */
public inline fun LongArray.forEach(operation: (Long) -> Unit) : Unit {
    for (element in this) operation(element)
}

/**
 * Folds all elements from from left to right with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> LongArray.fold(initial: R, operation: (R, Long) -> R) : R {
    var answer = initial
    for (element in this) answer = operation(answer, element)
    return answer
}

/**
 * Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> LongArray.foldRight(initial: R, operation: (Long, R) -> R) : R {
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
public inline fun LongArray.reduce(operation: (Long, Long) -> Long) : Long {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw UnsupportedOperationException("Empty iterable can't be reduced")
    }
    
    var result: Long = iterator.next() //compiler doesn't understand that result will initialized anyway
    while (iterator.hasNext()) {
        result = operation(result, iterator.next())
    }
    
    return result
}

/**
 * Applies binary operation to all elements of iterable, going from right to left.
 * Similar to foldRight function, but uses the last element as initial value
 */
public inline fun LongArray.reduceRight(operation: (Long, Long) -> Long) : Long {
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
public inline fun <K> LongArray.groupBy(toKey: (Long) -> K) : Map<K, List<Long>> {
    return groupByTo(HashMap<K, MutableList<Long>>(), toKey)
}

public inline fun <K> LongArray.groupByTo(result: MutableMap<K, MutableList<Long>>, toKey: (Long) -> K) : Map<K, MutableList<Long>> {
    for (element in this) {
        val key = toKey(element)
        val list = result.getOrPut(key) { ArrayList<Long>() }
        list.add(element)
    }
    return result
}

/**
 * Returns a list containing everything but the first *n* elements
 */
public inline fun LongArray.drop(n: Int) : List<Long> {
    return dropWhile(countTo(n))
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun LongArray.dropWhile(predicate: (Long) -> Boolean) : List<Long> {
    return dropWhileTo(ArrayList<Long>(), predicate)
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun <L: MutableList<in Long>> LongArray.dropWhileTo(result: L, predicate: (Long) -> Boolean) : L {
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
public inline fun LongArray.take(n: Int) : List<Long> {
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun LongArray.takeWhile(predicate: (Long) -> Boolean) : List<Long> {
    return takeWhileTo(ArrayList<Long>(), predicate)
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun <C: MutableCollection<in Long>> LongArray.takeWhileTo(result: C, predicate: (Long) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element) else break
    return result
}

/**
 * Copies all elements into the given collection
 */
public inline fun <C: MutableCollection<in Long>> LongArray.toCollection(result: C) : C {
    for (element in this) result.add(element)
    return result
}

/**
 * Reverses the order the elements into a list
 */
public inline fun LongArray.reverse() : List<Long> {
    val list = toCollection(ArrayList<Long>())
    Collections.reverse(list)
    return list
}

/**
 * Copies all elements into a [[LinkedList]]
 */
public inline fun LongArray.toLinkedList() : LinkedList<Long> {
    return toCollection(LinkedList<Long>())
}

/**
 * Copies all elements into a [[List]]
 */
public inline fun LongArray.toList() : List<Long> {
    return toCollection(ArrayList<Long>())
}

/**
 * Copies all elements into a [[Set]]
 */
public inline fun LongArray.toSet() : Set<Long> {
    return toCollection(LinkedHashSet<Long>())
}

/**
 * Copies all elements into a [[SortedSet]]
 */
public inline fun LongArray.toSortedSet() : SortedSet<Long> {
    return toCollection(TreeSet<Long>())
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the given element at the end
 */
public inline fun LongArray.plus(element: Long) : List<Long> {
    val answer = ArrayList<Long>()
    toCollection(answer)
    answer.add(element)
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following iterator
 */
public inline fun LongArray.plus(iterator: Iterator<Long>) : List<Long> {
    val answer = ArrayList<Long>()
    toCollection(answer)
    for (element in iterator) {
        answer.add(element)
    }
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following collection
 */
public inline fun LongArray.plus(collection: Iterable<Long>) : List<Long> {
    return plus(collection.iterator())
}

/**
 * Returns an iterator of Pairs(index, data)
 */
public inline fun LongArray.withIndices() : Iterator<Pair<Int, Long>> {
    return IndexIterator(iterator())
}

/**
 * Copies all elements into a [[List]] and sorts it by value of compare_function(element)
 * E.g. arrayList("two" to 2, "one" to 1).sortBy({it._2}) returns list sorted by second element of tuple
 */
public inline fun <R: Comparable<R>> LongArray.sortBy(f: (Long) -> R) : List<Long> {
    val sortedList = toCollection(ArrayList<Long>())
    val sortBy: Comparator<Long> = comparator<Long> {(x: Long, y: Long) ->
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
public inline fun LongArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String ="", postfix: String = "", limit: Int = -1, truncated: String = "...") : Unit {
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
public inline fun LongArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "...") : String {
    val buffer = StringBuilder()
    appendString(buffer, separator, prefix, postfix, limit, truncated)
    return buffer.toString()
}

