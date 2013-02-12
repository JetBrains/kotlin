package kotlin

import java.util.*

/**
 * Returns *true* if all elements match the given *predicate*
 */
public inline fun CharArray.all(predicate: (Char) -> Boolean) : Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns *true* if any elements match the given *predicate*
 */
public inline fun CharArray.any(predicate: (Char) -> Boolean) : Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of elements which match the given *predicate*
 */
public inline fun CharArray.count(predicate: (Char) -> Boolean) : Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Returns the first element which matches the given *predicate* or *null* if none matched
 */
public inline fun CharArray.find(predicate: (Char) -> Boolean) : Char? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns a list containing all elements which match the given *predicate*
 */
public inline fun CharArray.filter(predicate: (Char) -> Boolean) : List<Char> {
    return filterTo(ArrayList<Char>(), predicate)
}

/**
 * Filters all elements which match the given predicate into the given list
 */
public inline fun <C: MutableCollection<in Char>> CharArray.filterTo(result: C, predicate: (Char) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element)
    return result
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun CharArray.filterNot(predicate: (Char) -> Boolean) : List<Char> {
    return filterNotTo(ArrayList<Char>(), predicate)
}

/**
 * Returns a list containing all elements which do not match the given *predicate*
 */
public inline fun <C: MutableCollection<in Char>> CharArray.filterNotTo(result: C, predicate: (Char) -> Boolean) : C {
    for (element in this) if (!predicate(element)) result.add(element)
    return result
}

/**
 * Partitions this collection into a pair of collections
 */
public inline fun CharArray.partition(predicate: (Char) -> Boolean) : Pair<List<Char>, List<Char>> {
    val first = ArrayList<Char>()
    val second = ArrayList<Char>()
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
public inline fun <R> CharArray.map(transform : (Char) -> R) : List<R> {
    return mapTo(ArrayList<R>(), transform)
}

/**
 * Transforms each element of this collection with the given *transform* function and
 * adds each return value to the given *results* collection
 */
public inline fun <R, C: MutableCollection<in R>> CharArray.mapTo(result: C, transform : (Char) -> R) : C {
    for (item in this)
        result.add(transform(item))
    return result
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single list
 */
public inline fun <R> CharArray.flatMap(transform: (Char)-> Iterable<R>) : List<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
 * Returns the result of transforming each element to one or more values which are concatenated together into a single collection
 */
public inline fun <R, C: MutableCollection<in R>> CharArray.flatMapTo(result: C, transform: (Char) -> Iterable<R>) : C {
    for (element in this) {
        val list = transform(element)
        for (r in list) result.add(r)
    }
    return result
}

/**
 * Performs the given *operation* on each element
 */
public inline fun CharArray.forEach(operation: (Char) -> Unit) : Unit {
    for (element in this) operation(element)
}

/**
 * Folds all elements from from left to right with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> CharArray.fold(initial: R, operation: (R, Char) -> R) : R {
    var answer = initial
    for (element in this) answer = operation(answer, element)
    return answer
}

/**
 * Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements
 */
public inline fun <R> CharArray.foldRight(initial: R, operation: (Char, R) -> R) : R {
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
public inline fun CharArray.reduce(operation: (Char, Char) -> Char) : Char {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw UnsupportedOperationException("Empty iterable can't be reduced")
    }
    
    var result: Char = iterator.next() //compiler doesn't understand that result will initialized anyway
    while (iterator.hasNext()) {
        result = operation(result, iterator.next())
    }
    
    return result
}

/**
 * Applies binary operation to all elements of iterable, going from right to left.
 * Similar to foldRight function, but uses the last element as initial value
 */
public inline fun CharArray.reduceRight(operation: (Char, Char) -> Char) : Char {
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
public inline fun <K> CharArray.groupBy(toKey: (Char) -> K) : Map<K, List<Char>> {
    return groupByTo(HashMap<K, MutableList<Char>>(), toKey)
}

public inline fun <K> CharArray.groupByTo(result: MutableMap<K, MutableList<Char>>, toKey: (Char) -> K) : Map<K, MutableList<Char>> {
    for (element in this) {
        val key = toKey(element)
        val list = result.getOrPut(key) { ArrayList<Char>() }
        list.add(element)
    }
    return result
}

/**
 * Returns a list containing everything but the first *n* elements
 */
public inline fun CharArray.drop(n: Int) : List<Char> {
    return dropWhile(countTo(n))
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun CharArray.dropWhile(predicate: (Char) -> Boolean) : List<Char> {
    return dropWhileTo(ArrayList<Char>(), predicate)
}

/**
 * Returns a list containing the everything but the first elements that satisfy the given *predicate*
 */
public inline fun <L: MutableList<in Char>> CharArray.dropWhileTo(result: L, predicate: (Char) -> Boolean) : L {
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
public inline fun CharArray.take(n: Int) : List<Char> {
    return takeWhile(countTo(n))
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun CharArray.takeWhile(predicate: (Char) -> Boolean) : List<Char> {
    return takeWhileTo(ArrayList<Char>(), predicate)
}

/**
 * Returns a list containing the first elements that satisfy the given *predicate*
 */
public inline fun <C: MutableCollection<in Char>> CharArray.takeWhileTo(result: C, predicate: (Char) -> Boolean) : C {
    for (element in this) if (predicate(element)) result.add(element) else break
    return result
}

/**
 * Copies all elements into the given collection
 */
public inline fun <C: MutableCollection<in Char>> CharArray.toCollection(result: C) : C {
    for (element in this) result.add(element)
    return result
}

/**
 * Reverses the order the elements into a list
 */
public inline fun CharArray.reverse() : List<Char> {
    val list = toCollection(ArrayList<Char>())
    Collections.reverse(list)
    return list
}

/**
 * Copies all elements into a [[LinkedList]]
 */
public inline fun CharArray.toLinkedList() : LinkedList<Char> {
    return toCollection(LinkedList<Char>())
}

/**
 * Copies all elements into a [[List]]
 */
public inline fun CharArray.toList() : List<Char> {
    return toCollection(ArrayList<Char>())
}

/**
 * Copies all elements into a [[Set]]
 */
public inline fun CharArray.toSet() : Set<Char> {
    return toCollection(LinkedHashSet<Char>())
}

/**
 * Copies all elements into a [[SortedSet]]
 */
public inline fun CharArray.toSortedSet() : SortedSet<Char> {
    return toCollection(TreeSet<Char>())
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the given element at the end
 */
public inline fun CharArray.plus(element: Char) : List<Char> {
    val answer = ArrayList<Char>()
    toCollection(answer)
    answer.add(element)
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following iterator
 */
public inline fun CharArray.plus(iterator: Iterator<Char>) : List<Char> {
    val answer = ArrayList<Char>()
    toCollection(answer)
    for (element in iterator) {
        answer.add(element)
    }
    return answer
}

/**
 * Creates an [[Iterator]] which iterates over this iterator then the following collection
 */
public inline fun CharArray.plus(collection: Iterable<Char>) : List<Char> {
    return plus(collection.iterator())
}

/**
 * Returns an iterator of Pairs(index, data)
 */
public inline fun CharArray.withIndices() : Iterator<Pair<Int, Char>> {
    return IndexIterator(iterator())
}

/**
 * Copies all elements into a [[List]] and sorts it by value of compare_function(element)
 * E.g. arrayList("two" to 2, "one" to 1).sortBy({it._2}) returns list sorted by second element of tuple
 */
public inline fun <R: Comparable<R>> CharArray.sortBy(f: (Char) -> R) : List<Char> {
    val sortedList = toCollection(ArrayList<Char>())
    val sortBy: Comparator<Char> = comparator<Char> {(x: Char, y: Char) ->
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
public inline fun CharArray.appendString(buffer: Appendable, separator: String = ", ", prefix: String ="", postfix: String = "", limit: Int = -1, truncated: String = "...") : Unit {
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
public inline fun CharArray.makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "...") : String {
    val buffer = StringBuilder()
    appendString(buffer, separator, prefix, postfix, limit, truncated)
    return buffer.toString()
}

