package kotlin

import java.util.*

/**
 * Returns *true* if all elements match the given *predicate*
 */
public inline fun DoubleArray.all(predicate: (Double) -> Boolean) : Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns *true* if any elements match the given *predicate*
 */
public inline fun DoubleArray.any(predicate: (Double) -> Boolean) : Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of elements which match the given *predicate*
 */
public inline fun DoubleArray.count(predicate: (Double) -> Boolean) : Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the first element which matches the given *predicate* or *null* if none matched
 */
public inline fun DoubleArray.find(predicate: (Double) -> Boolean) : Double? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns a list containing all elements which match the given *predicate*
 */
public inline fun DoubleArray.filter(predicate: (Double) -> Boolean) : List<Double> {
    return filterTo(ArrayList<Double>(), predicate)
}

/**
 * Filters all elements which match the given predicate into the given list
 */
public inline fun <C: MutableCollection<in Double>> DoubleArray.filterTo(result: C, predicate: (Double) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element)
    return result
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun DoubleArray.filterNot(predicate: (Double) -> Boolean) : List<Double> {
    return filterNotTo(ArrayList<Double>(), predicate)
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun <C: MutableCollection<in Double>> DoubleArray.filterNotTo(result: C, predicate: (Double) -> Boolean) : C {
    for (element in this) if (!predicate(element)) result.add(element)
    return result
}

/**
 * Partitions this collection into a pair of collections
 */
public inline fun DoubleArray.partition(predicate: (Double) -> Boolean) : Pair<List<Double>, List<Double>> {
    val first = ArrayList<Double>()
    val second = ArrayList<Double>()
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
public inline fun <R> DoubleArray.map(transform : (Double) -> R) : List<R> {
    return mapTo(ArrayList<R>(), transform)
}

/**
 * Transforms each element of this collection with the given *transform* function and
 * adds each return value to the given *results* collection
 */
public inline fun <R, C: MutableCollection<in R>> DoubleArray.mapTo(result: C, transform : (Double) -> R) : C {
    for (item in this)
        result.add(transform(item))
    return result
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single list
 */
public inline fun <R> DoubleArray.flatMap(transform: (Double)-> Iterable<R>) : List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 */
public inline fun <R, C: MutableCollection<in R>> DoubleArray.flatMapTo(result: C, transform: (Double) -> Iterable<R>) : C {
    for (element in this) {
        val list = transform(element)
        for (r in list) result.add(r)
    }
    return result
}

/**
 * Performs the given *operation* on each element
 */
public inline fun DoubleArray.forEach(operation: (Double) -> Unit) : Unit {
    for (element in this) operation(element)
}

/**
 * Folds all elements from from left to right with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> DoubleArray.fold(initial: R, operation: (R, Double) -> R) : R {
    var answer = initial
    for (element in this) answer = operation(answer, element)
    return answer
}

/**
 * Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> DoubleArray.foldRight(initial: R, operation: (Double, R) -> R) : R {
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
public inline fun DoubleArray.reduce(operation: (Double, Double) -> Double) : Double {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw UnsupportedOperationException("Empty iterable can't be reduced")
    }
    
    var result: Double = iterator.next() //compiler doesn't understand that result will initialized anyway
    while (iterator.hasNext()) {
        result = operation(result, iterator.next())
    }
    
    return result
}

/**
 * Applies binary operation to all elements of iterable, going from right to left.
 * Similar to foldRight function, but uses the last element as initial value
 */
public inline fun DoubleArray.reduceRight(operation: (Double, Double) -> Double) : Double {
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
public inline fun <K> DoubleArray.groupBy(toKey: (Double) -> K) : Map<K, List<Double>> {
    return groupByTo(HashMap<K, MutableList<Double>>(), toKey)
}

public inline fun <K> DoubleArray.groupByTo(result: MutableMap<K, MutableList<Double>>, toKey: (Double) -> K) : Map<K, MutableList<Double>> {
    for (element in this) {
        val key = toKey(element)
        val list = result.getOrPut(key) { ArrayList<Double>() }
        list.add(element)
    }
    return result
}

/**
 * Returns a list containing everything but the first *n* elements
 */
public inline fun DoubleArray.drop(n: Int) : List<Double> {
    return dropWhile(countTo(n))
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun DoubleArray.dropWhile(predicate: (Double) -> Boolean) : List<Double> {
    return dropWhileTo(ArrayList<Double>(), predicate)
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun <L: MutableList<in Double>> DoubleArray.dropWhileTo(result: L, predicate: (Double) -> Boolean) : L {
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
public inline fun DoubleArray.take(n: Int) : List<Double> {
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun DoubleArray.takeWhile(predicate: (Double) -> Boolean) : List<Double> {
    return takeWhileTo(ArrayList<Double>(), predicate)
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun <C: MutableCollection<in Double>> DoubleArray.takeWhileTo(result: C, predicate: (Double) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element) else break
    return result
}

/**
 * Copies all elements into the given collection
 */
public inline fun <C: MutableCollection<in Double>> DoubleArray.toCollection(result: C) : C {
    for (element in this) result.add(element)
    return result
}

/**
 * Reverses the order the elements into a list
 */
public inline fun DoubleArray.reverse() : List<Double> {
    val list = toCollection(ArrayList<Double>())
    Collections.reverse(list)
    return list
}

/**
 * Copies all elements into a [[LinkedList]]
 */
public inline fun DoubleArray.toLinkedList() : LinkedList<Double> {
    return toCollection(LinkedList<Double>())
}

/**
 * Copies all elements into a [[List]]
 */
public inline fun DoubleArray.toList() : List<Double> {
    return toCollection(ArrayList<Double>())
}

/**
 * Copies all elements into a [[Set]]
 */
public inline fun DoubleArray.toSet() : Set<Double> {
    return toCollection(LinkedHashSet<Double>())
}

/**
 * Copies all elements into a [[SortedSet]]
 */
public inline fun DoubleArray.toSortedSet() : SortedSet<Double> {
    return toCollection(TreeSet<Double>())
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the given element at the end
 */
public inline fun DoubleArray.plus(element: Double) : List<Double> {
    val answer = ArrayList<Double>()
    toCollection(answer)
    answer.add(element)
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following iterator
 */
public inline fun DoubleArray.plus(iterator: Iterator<Double>) : List<Double> {
    val answer = ArrayList<Double>()
    toCollection(answer)
    for (element in iterator) {
        answer.add(element)
    }
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following collection
 */
public inline fun DoubleArray.plus(collection: Iterable<Double>) : List<Double> {
    return plus(collection.iterator())
}

/**
 * Returns an iterator of Pairs(index, data)
 */
public inline fun DoubleArray.withIndices() : Iterator<Pair<Int, Double>> {
    return IndexIterator(iterator())
}

/**
 * Copies all elements into a [[List]] and sorts it by value of compare_function(element)
 * E.g. arrayList("two" to 2, "one" to 1).sortBy({it._2}) returns list sorted by second element of tuple
 */
public inline fun <R: Comparable<R>> DoubleArray.sortBy(f: (Double) -> R) : List<Double> {
    val sortedList = toCollection(ArrayList<Double>())
    val sortBy: Comparator<Double> = comparator<Double> {(x: Double, y: Double) ->
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
public inline fun DoubleArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String ="", postfix: String = "", limit: Int = -1, truncated: String = "...") : Unit {
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
public inline fun DoubleArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "...") : String {
    val buffer = StringBuilder()
    appendString(buffer, separator, prefix, postfix, limit, truncated)
    return buffer.toString()
}

