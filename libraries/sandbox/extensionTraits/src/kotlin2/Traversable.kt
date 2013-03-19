package kotlin2

import java.util.*

public trait Traversable<T>: Iterable<T> {

    /**
    * Returns *true* if all elements match the given *predicate*
    *
    * @includeFunctionBody ../../test/CollectionTest.kt all
    */
    public inline fun all(predicate: (T) -> Boolean): Boolean {
        for (element in this) if (!predicate(element)) return false
        return true
    }

    /**
    * Returns *true* if any elements match the given *predicate*
    *
    * @includeFunctionBody ../../test/CollectionTest.kt any
    */
    public inline fun any(predicate: (T) -> Boolean): Boolean {
        for (element in this) if (predicate(element)) return true
        return false
    }

    /**
    * Appends the string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied
    *
    * If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
    * a special *truncated* separator (which defaults to "..."
    *
    * @includeFunctionBody ../../test/CollectionTest.kt appendString
    */
    public inline fun appendString(buffer: Appendable, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): Unit {
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
    * Returns the number of elements which match the given *predicate*
    *
    * @includeFunctionBody ../../test/CollectionTest.kt count
    */
    public fun count(predicate: (T) -> Boolean): Int {
        var count = 0
        for (element in this) if (predicate(element)) count++
        return count
    }

    /**
    * Returns the first element which matches the given *predicate* or *null* if none matched
    *
    * @includeFunctionBody ../../test/CollectionTest.kt find
    */
    public inline fun find(predicate: (T) -> Boolean): T? {
        for (element in this) if (predicate(element)) return element
        return null
    }

    /**
    * Filters all elements which match the given predicate into the given list
    *
    * @includeFunctionBody ../../test/CollectionTest.kt filterIntoLinkedList
    */
    public inline fun <C: Collection<in T>> filterTo(result: C, predicate: (T) -> Boolean): C {
        for (element in this) if (predicate(element)) result.add(element)
        return result
    }

    /**
    * Returns a list containing all elements which do not match the given *predicate*
    *
    * @includeFunctionBody ../../test/CollectionTest.kt filterNotIntoLinkedList
    */
    public inline fun <C: Collection<in T>> filterNotTo(result: C, predicate: (T) -> Boolean): C {
        for (element in this) if (!predicate(element)) result.add(element)
        return result
    }

    /**
    * Filters all non-*null* elements into the given list
    *
    * @includeFunctionBody ../../test/CollectionTest.kt filterNotNullIntoLinkedList
    */
    public inline fun <C: Collection<in T>> java.lang.Iterable<T?>?.filterNotNullTo(result: C): C {
        if (this != null) {
            for (element in this) if (element != null) result.add(element)
        }
        return result
    }

    /**
    * Returns the result of transforming each element to one or more values which are concatenated together into a single list
    *
    * @includeFunctionBody ../../test/CollectionTest.kt flatMap
    */
    public inline fun <R> flatMapTo(result: Collection<R>, transform: (T) -> Collection<R>): Collection<R> {
        for (element in this) {
            val list = transform(element)
            if (list != null) {
                for (r in list) result.add(r)
            }
        }
        return result
    }

    /**
    * Performs the given *operation* on each element
    *
    * @includeFunctionBody ../../test/CollectionTest.kt forEach
    */
    public inline fun forEach(operation: (T) -> Unit): Unit = for (element in this) operation(element)

    /**
    * Folds all elements from from left to right with the *initial* value to perform the operation on sequential pairs of elements
    *
    * @includeFunctionBody ../../test/CollectionTest.kt fold
    */
    public inline fun fold(initial: T, operation: (T, T) -> T): T {
        var answer = initial
        for (element in this) answer = operation(answer, element)
        return answer
    }

    /**
    * Folds all elements from right to left with the *initial* value to perform the operation on sequential pairs of elements
    *
    * @includeFunctionBody ../../test/CollectionTest.kt foldRight
    */
    public inline fun foldRight(initial: T, operation: (T, T) -> T): T = reverse().fold(initial, {x, y -> operation(y, x)})


    /**
    * Applies binary operation to all elements of iterable, going from left to right.
    * Similar to fold function, but uses the first element as initial value
    *
    * @includeFunctionBody ../../test/CollectionTest.kt reduce
    */
    public inline fun reduce(operation: (T, T) -> T): T {
        val iter = this.iterator()!!
        if (!iter.hasNext) {
            throw UnsupportedOperationException("Empty iterable can't be reduced")
        }

        var result: T = iter.next() //compiler doesn't understand that result will initialized anyway
        while (iter.hasNext) {
            result = operation(result, iter.next())
        }

        return result
    }

    /**
    * Applies binary operation to all elements of iterable, going from right to left.
    * Similar to foldRight function, but uses the last element as initial value
    *
    * @includeFunctionBody ../../test/CollectionTest.kt reduceRight
    */
    public inline fun reduceRight(operation: (T, T) -> T): T = reverse().reduce { x, y -> operation(y, x) }


    /**
    * Groups the elements in the collection into a new [[Map]] using the supplied *toKey* function to calculate the key to group the elements by
    *
    * @includeFunctionBody ../../test/CollectionTest.kt groupBy
    */
    public inline fun <K> groupBy(toKey: (T) -> K): Map<K, List<T>> = groupByTo<K>(HashMap<K, List<T>>(), toKey)

    /**
    * Groups the elements in the collection into the given [[Map]] using the supplied *toKey* function to calculate the key to group the elements by
    *
    * @includeFunctionBody ../../test/CollectionTest.kt groupBy
    */
    public inline fun <K> groupByTo(result: Map<K, List<T>>, toKey: (T) -> K): Map<K, List<T>> {
        for (element in this) {
            val key = toKey(element)
            val list = result.getOrPut(key) { ArrayList<T>() }
            list.add(element)
        }
        return result
    }

    /**
    * Creates a string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied.
    *
    * If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
    * a special *truncated* separator (which defaults to "..."
    *
    * @includeFunctionBody ../../test/CollectionTest.kt makeString
    */
    public inline fun makeString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
        val buffer = StringBuilder()
        appendString(buffer, separator, prefix, postfix, limit, truncated)
        return buffer.toString()!!
    }

    /** Returns a list containing the everything but the first elements that satisfy the given *predicate* */
    public inline fun <L: List<in T>> dropWhileTo(result: L, predicate: (T) -> Boolean): L {
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

    /** Returns a list containing the first elements that satisfy the given *predicate* */
    public inline fun <C: Collection<in T>> takeWhileTo(result: C, predicate: (T) -> Boolean): C {
        for (element in this) if (predicate(element)) result.add(element) else break
        return result
    }

    /** Copies all elements into the given collection */
    public inline fun <C: Collection<in T>> toCollection(result: C): C {
        for (element in this) result.add(element)
        return result
    }

    /**
    * Reverses the order the elements into a list
    *
    * @includeFunctionBody ../../test/CollectionTest.kt reverse
    */
    public inline fun reverse(): List<T> {
        val list = toList()
        Collections.reverse(list)
        return list
    }

    /** Copies all elements into a [[LinkedList]]  */
    public inline fun toLinkedList(): LinkedList<T> = toCollection(LinkedList<T>())

    /** Copies all elements into a [[List]] */
    public inline fun toList(): List<T> = toCollection(ArrayList<T>())

    /** Copies all elements into a [[List] */
    public inline fun toCollection(): Collection<T> = toCollection(ArrayList<T>())

    /** Copies all elements into a [[Set]] */
    public inline fun toSet(): Set<T> = toCollection(HashSet<T>())

    /**
      TODO figure out necessary variance/generics ninja stuff... :)
    public inline fun toSortedList(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
        val answer = this.toList()
        answer.sort(transform)
        return answer
    }
    */


    /**
    * Returns a list containing everything but the first *n* elements
    *
    * @includeFunctionBody ../../test/CollectionTest.kt drop
    */
    public inline fun drop(n: Int): List<T> {
        return dropWhile(countTo(n))
    }

    /**
    * Returns a list containing the everything but the first elements that satisfy the given *predicate*
    *
    * @includeFunctionBody ../../test/CollectionTest.kt dropWhile
    */
    public inline fun dropWhile(predicate: (T) -> Boolean): List<T> = dropWhileTo(ArrayList<T>(), predicate)

    /**
    * Count the number of elements in collection.
    *
    * If base collection implements [[Collection]] interface method [[Collection.size()]] will be used.
    * Otherwise, this method determines the count by iterating through the all items.
    */
    public fun count(): Int {
        var number: Int = 0
        for (elem in this) {
            ++number
        }
        return number
    }

    /**
    * Get the first element in the collection.
    *
    * Will throw an exception if there are no elements
    */
    // TODO: Specify type of the exception
    public fun first(): T {
        return this.iterator()!!.next()
    }

    /**
    * Get the last element in the collection.
    *
    * If base collection implements [[List]] interface, the combination of size() and get()
    * methods will be used for getting last element. Otherwise, this method determines the
    * last item by iterating through the all items.
    *
    * Will throw an exception if there are no elements.
    *
    * @includeFunctionBody ../../test/CollectionTest.kt last
    */
    // TODO: Specify type of the exception
    public fun last(): T {
        val iterator = this.iterator()!!
        var last: T = iterator.next()

        while (iterator.hasNext) {
            last = iterator.next()
        }

        return last;
    }

    /**
    * Checks if collection contains given item.
    *
    * Method checks equality of the objects with T.equals method.
    * If collection implements [[java.util.AbstractCollection]] an overridden implementation of the contains
    * method will be used.
    */
    public fun contains(item: T): Boolean {
        for (var elem in this) {
            if (elem == item) {
                return true
            }
        }
        return false
    }

    /**
    * Convert collection of arbitrary elements to collection of pairs of the index and the element
    *
    * @includeFunctionBody ../../test/ListTest.kt withIndices
    */
    /*
        public fun withIndices(): java.lang.Iterable<Pair(Int, T)> {
            return object : java.lang.Iterable<Pair(Int, T)> {
                public override fun iterator(): Iterator<Pair(Int, T)> {
                    return NumberedIterator<T>(iterator())
                }
            }
        }
    */
}


