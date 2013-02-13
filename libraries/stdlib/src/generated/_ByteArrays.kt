package kotlin

import java.util.*

/**
 * Returns *true* if all elements match the given *predicate*
 */
public inline fun ByteArray.all(predicate: (Byte) -> Boolean) : Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns *true* if any elements match the given *predicate*
 */
public inline fun ByteArray.any(predicate: (Byte) -> Boolean) : Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of elements which match the given *predicate*
 */
public inline fun ByteArray.count(predicate: (Byte) -> Boolean) : Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the first element which matches the given *predicate* or *null* if none matched
 */
public inline fun ByteArray.find(predicate: (Byte) -> Boolean) : Byte? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns a list containing all elements which match the given *predicate*
 */
public inline fun ByteArray.filter(predicate: (Byte) -> Boolean) : List<Byte> {
    return filterTo(ArrayList<Byte>(), predicate)
}

/**
 * Filters all elements which match the given predicate into the given list
 */
public inline fun <C: MutableCollection<in Byte>> ByteArray.filterTo(result: C, predicate: (Byte) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element)
    return result
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun ByteArray.filterNot(predicate: (Byte) -> Boolean) : List<Byte> {
    return filterNotTo(ArrayList<Byte>(), predicate)
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun <C: MutableCollection<in Byte>> ByteArray.filterNotTo(result: C, predicate: (Byte) -> Boolean) : C {
    for (element in this) if (!predicate(element)) result.add(element)
    return result
}

/**
 * Partitions this collection into a pair of collections
 */
public inline fun ByteArray.partition(predicate: (Byte) -> Boolean) : Pair<List<Byte>, List<Byte>> {
    val first = ArrayList<Byte>()
    val second = ArrayList<Byte>()
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
public inline fun <R> ByteArray.map(transform : (Byte) -> R) : List<R> {
    return mapTo(ArrayList<R>(), transform)
}

/**
 * Transforms each element of this collection with the given *transform* function and
 * adds each return value to the given *results* collection
 */
public inline fun <R, C: MutableCollection<in R>> ByteArray.mapTo(result: C, transform : (Byte) -> R) : C {
    for (item in this)
        result.add(transform(item))
    return result
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single list
 */
public inline fun <R> ByteArray.flatMap(transform: (Byte)-> Iterable<R>) : List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 */
public inline fun <R, C: MutableCollection<in R>> ByteArray.flatMapTo(result: C, transform: (Byte) -> Iterable<R>) : C {
    for (element in this) {
        val list = transform(element)
        for (r in list) result.add(r)
    }
    return result
}

/**
 * Performs the given *operation* on each element
 */
public inline fun ByteArray.forEach(operation: (Byte) -> Unit) : Unit {
    for (element in this) operation(element)
}

/**
 * Folds all elements from from left to right with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> ByteArray.fold(initial: R, operation: (R, Byte) -> R) : R {
    var answer = initial
    for (element in this) answer = operation(answer, element)
    return answer
}

/**
 * Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> ByteArray.foldRight(initial: R, operation: (Byte, R) -> R) : R {
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
public inline fun ByteArray.reduce(operation: (Byte, Byte) -> Byte) : Byte {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw UnsupportedOperationException("Empty iterable can't be reduced")
    }
    
    var result: Byte = iterator.next() //compiler doesn't understand that result will initialized anyway
    while (iterator.hasNext()) {
        result = operation(result, iterator.next())
    }
    
    return result
}

/**
 * Applies binary operation to all elements of iterable, going from right to left.
 * Similar to foldRight function, but uses the last element as initial value
 */
public inline fun ByteArray.reduceRight(operation: (Byte, Byte) -> Byte) : Byte {
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
public inline fun <K> ByteArray.groupBy(toKey: (Byte) -> K) : Map<K, List<Byte>> {
    return groupByTo(HashMap<K, MutableList<Byte>>(), toKey)
}

public inline fun <K> ByteArray.groupByTo(result: MutableMap<K, MutableList<Byte>>, toKey: (Byte) -> K) : Map<K, MutableList<Byte>> {
    for (element in this) {
        val key = toKey(element)
        val list = result.getOrPut(key) { ArrayList<Byte>() }
        list.add(element)
    }
    return result
}

/**
 * Returns a list containing everything but the first *n* elements
 */
public inline fun ByteArray.drop(n: Int) : List<Byte> {
    return dropWhile(countTo(n))
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun ByteArray.dropWhile(predicate: (Byte) -> Boolean) : List<Byte> {
    return dropWhileTo(ArrayList<Byte>(), predicate)
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun <L: MutableList<in Byte>> ByteArray.dropWhileTo(result: L, predicate: (Byte) -> Boolean) : L {
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
public inline fun ByteArray.take(n: Int) : List<Byte> {
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun ByteArray.takeWhile(predicate: (Byte) -> Boolean) : List<Byte> {
    return takeWhileTo(ArrayList<Byte>(), predicate)
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun <C: MutableCollection<in Byte>> ByteArray.takeWhileTo(result: C, predicate: (Byte) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element) else break
    return result
}

/**
 * Copies all elements into the given collection
 */
public inline fun <C: MutableCollection<in Byte>> ByteArray.toCollection(result: C) : C {
    for (element in this) result.add(element)
    return result
}

/**
 * Reverses the order the elements into a list
 */
public inline fun ByteArray.reverse() : List<Byte> {
    val list = toCollection(ArrayList<Byte>())
    Collections.reverse(list)
    return list
}

/**
 * Copies all elements into a [[LinkedList]]
 */
public inline fun ByteArray.toLinkedList() : LinkedList<Byte> {
    return toCollection(LinkedList<Byte>())
}

/**
 * Copies all elements into a [[List]]
 */
public inline fun ByteArray.toList() : List<Byte> {
    return toCollection(ArrayList<Byte>())
}

/**
 * Copies all elements into a [[Set]]
 */
public inline fun ByteArray.toSet() : Set<Byte> {
    return toCollection(LinkedHashSet<Byte>())
}

/**
 * Copies all elements into a [[SortedSet]]
 */
public inline fun ByteArray.toSortedSet() : SortedSet<Byte> {
    return toCollection(TreeSet<Byte>())
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the given element at the end
 */
public inline fun ByteArray.plus(element: Byte) : List<Byte> {
    val answer = ArrayList<Byte>()
    toCollection(answer)
    answer.add(element)
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following iterator
 */
public inline fun ByteArray.plus(iterator: Iterator<Byte>) : List<Byte> {
    val answer = ArrayList<Byte>()
    toCollection(answer)
    for (element in iterator) {
        answer.add(element)
    }
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following collection
 */
public inline fun ByteArray.plus(collection: Iterable<Byte>) : List<Byte> {
    return plus(collection.iterator())
}

/**
 * Returns an iterator of Pairs(index, data)
 */
public inline fun ByteArray.withIndices() : Iterator<Pair<Int, Byte>> {
    return IndexIterator(iterator())
}

/**
 * Copies all elements into a [[List]] and sorts it by value of compare_function(element)
 * E.g. arrayList("two" to 2, "one" to 1).sortBy({it._2}) returns list sorted by second element of tuple
 */
public inline fun <R: Comparable<R>> ByteArray.sortBy(f: (Byte) -> R) : List<Byte> {
    val sortedList = toCollection(ArrayList<Byte>())
    val sortBy: Comparator<Byte> = comparator<Byte> {(x: Byte, y: Byte) ->
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
public inline fun ByteArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String ="", postfix: String = "", limit: Int = -1, truncated: String = "...") : Unit {
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
public inline fun ByteArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "...") : String {
    val buffer = StringBuilder()
    appendString(buffer, separator, prefix, postfix, limit, truncated)
    return buffer.toString()
}

