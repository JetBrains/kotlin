package kotlin

import java.util.*

/**
 * Returns *true* if all elements match the given *predicate*
 */
public inline fun BooleanArray.all(predicate: (Boolean) -> Boolean) : Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns *true* if any elements match the given *predicate*
 */
public inline fun BooleanArray.any(predicate: (Boolean) -> Boolean) : Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of elements which match the given *predicate*
 */
public inline fun BooleanArray.count(predicate: (Boolean) -> Boolean) : Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the first element which matches the given *predicate* or *null* if none matched
 */
public inline fun BooleanArray.find(predicate: (Boolean) -> Boolean) : Boolean? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns a list containing all elements which match the given *predicate*
 */
public inline fun BooleanArray.filter(predicate: (Boolean) -> Boolean) : List<Boolean> {
    return filterTo(ArrayList<Boolean>(), predicate)
}

/**
 * Filters all elements which match the given predicate into the given list
 */
public inline fun <C: MutableCollection<in Boolean>> BooleanArray.filterTo(result: C, predicate: (Boolean) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element)
    return result
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun BooleanArray.filterNot(predicate: (Boolean) -> Boolean) : List<Boolean> {
    return filterNotTo(ArrayList<Boolean>(), predicate)
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun <C: MutableCollection<in Boolean>> BooleanArray.filterNotTo(result: C, predicate: (Boolean) -> Boolean) : C {
    for (element in this) if (!predicate(element)) result.add(element)
    return result
}

/**
 * Partitions this collection into a pair of collections
 */
public inline fun BooleanArray.partition(predicate: (Boolean) -> Boolean) : Pair<List<Boolean>, List<Boolean>> {
    val first = ArrayList<Boolean>()
    val second = ArrayList<Boolean>()
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
public inline fun <R> BooleanArray.map(transform : (Boolean) -> R) : List<R> {
    return mapTo(ArrayList<R>(), transform)
}

/**
 * Transforms each element of this collection with the given *transform* function and
 * adds each return value to the given *results* collection
 */
public inline fun <R, C: MutableCollection<in R>> BooleanArray.mapTo(result: C, transform : (Boolean) -> R) : C {
    for (item in this)
        result.add(transform(item))
    return result
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single list
 */
public inline fun <R> BooleanArray.flatMap(transform: (Boolean)-> Iterable<R>) : List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 */
public inline fun <R, C: MutableCollection<in R>> BooleanArray.flatMapTo(result: C, transform: (Boolean) -> Iterable<R>) : C {
    for (element in this) {
        val list = transform(element)
        for (r in list) result.add(r)
    }
    return result
}

/**
 * Performs the given *operation* on each element
 */
public inline fun BooleanArray.forEach(operation: (Boolean) -> Unit) : Unit {
    for (element in this) operation(element)
}

/**
 * Folds all elements from from left to right with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> BooleanArray.fold(initial: R, operation: (R, Boolean) -> R) : R {
    var answer = initial
    for (element in this) answer = operation(answer, element)
    return answer
}

/**
 * Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> BooleanArray.foldRight(initial: R, operation: (Boolean, R) -> R) : R {
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
public inline fun BooleanArray.reduce(operation: (Boolean, Boolean) -> Boolean) : Boolean {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw UnsupportedOperationException("Empty iterable can't be reduced")
    }
    
    var result: Boolean = iterator.next() //compiler doesn't understand that result will initialized anyway
    while (iterator.hasNext()) {
        result = operation(result, iterator.next())
    }
    
    return result
}

/**
 * Applies binary operation to all elements of iterable, going from right to left.
 * Similar to foldRight function, but uses the last element as initial value
 */
public inline fun BooleanArray.reduceRight(operation: (Boolean, Boolean) -> Boolean) : Boolean {
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
public inline fun <K> BooleanArray.groupBy(toKey: (Boolean) -> K) : Map<K, List<Boolean>> {
    return groupByTo(HashMap<K, MutableList<Boolean>>(), toKey)
}

public inline fun <K> BooleanArray.groupByTo(result: MutableMap<K, MutableList<Boolean>>, toKey: (Boolean) -> K) : Map<K, MutableList<Boolean>> {
    for (element in this) {
        val key = toKey(element)
        val list = result.getOrPut(key) { ArrayList<Boolean>() }
        list.add(element)
    }
    return result
}

/**
 * Returns a list containing everything but the first *n* elements
 */
public inline fun BooleanArray.drop(n: Int) : List<Boolean> {
    return dropWhile(countTo(n))
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun BooleanArray.dropWhile(predicate: (Boolean) -> Boolean) : List<Boolean> {
    return dropWhileTo(ArrayList<Boolean>(), predicate)
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun <L: MutableList<in Boolean>> BooleanArray.dropWhileTo(result: L, predicate: (Boolean) -> Boolean) : L {
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
public inline fun BooleanArray.take(n: Int) : List<Boolean> {
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun BooleanArray.takeWhile(predicate: (Boolean) -> Boolean) : List<Boolean> {
    return takeWhileTo(ArrayList<Boolean>(), predicate)
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun <C: MutableCollection<in Boolean>> BooleanArray.takeWhileTo(result: C, predicate: (Boolean) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element) else break
    return result
}

/**
 * Copies all elements into the given collection
 */
public inline fun <C: MutableCollection<in Boolean>> BooleanArray.toCollection(result: C) : C {
    for (element in this) result.add(element)
    return result
}

/**
 * Reverses the order the elements into a list
 */
public inline fun BooleanArray.reverse() : List<Boolean> {
    val list = toCollection(ArrayList<Boolean>())
    Collections.reverse(list)
    return list
}

/**
 * Copies all elements into a [[LinkedList]]
 */
public inline fun BooleanArray.toLinkedList() : LinkedList<Boolean> {
    return toCollection(LinkedList<Boolean>())
}

/**
 * Copies all elements into a [[List]]
 */
public inline fun BooleanArray.toList() : List<Boolean> {
    return toCollection(ArrayList<Boolean>())
}

/**
 * Copies all elements into a [[Set]]
 */
public inline fun BooleanArray.toSet() : Set<Boolean> {
    return toCollection(LinkedHashSet<Boolean>())
}

/**
 * Copies all elements into a [[SortedSet]]
 */
public inline fun BooleanArray.toSortedSet() : SortedSet<Boolean> {
    return toCollection(TreeSet<Boolean>())
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the given element at the end
 */
public inline fun BooleanArray.plus(element: Boolean) : List<Boolean> {
    val answer = ArrayList<Boolean>()
    toCollection(answer)
    answer.add(element)
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following iterator
 */
public inline fun BooleanArray.plus(iterator: Iterator<Boolean>) : List<Boolean> {
    val answer = ArrayList<Boolean>()
    toCollection(answer)
    for (element in iterator) {
        answer.add(element)
    }
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following collection
 */
public inline fun BooleanArray.plus(collection: Iterable<Boolean>) : List<Boolean> {
    return plus(collection.iterator())
}

/**
 * Returns an iterator of Pairs(index, data)
 */
public inline fun BooleanArray.withIndices() : Iterator<Pair<Int, Boolean>> {
    return IndexIterator(iterator())
}

/**
 * Copies all elements into a [[List]] and sorts it by value of compare_function(element)
 * E.g. arrayList("two" to 2, "one" to 1).sortBy({it._2}) returns list sorted by second element of tuple
 */
public inline fun <R: Comparable<R>> BooleanArray.sortBy(f: (Boolean) -> R) : List<Boolean> {
    val sortedList = toCollection(ArrayList<Boolean>())
    val sortBy: Comparator<Boolean> = comparator<Boolean> {(x: Boolean, y: Boolean) ->
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
public inline fun BooleanArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String ="", postfix: String = "", limit: Int = -1, truncated: String = "...") : Unit {
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
public inline fun BooleanArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "...") : String {
    val buffer = StringBuilder()
    appendString(buffer, separator, prefix, postfix, limit, truncated)
    return buffer.toString()
}

