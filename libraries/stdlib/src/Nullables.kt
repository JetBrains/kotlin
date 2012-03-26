package kotlin.nullable

import java.util.*

/** Returns true if the element is not null and matches the given predicate */
inline fun <T> T?.any(predicate: (T)-> Boolean): Boolean {
    return this != null && predicate(this)
}

/** Returns true if the element is not null and matches the given predicate */
inline fun <T> T?.all(predicate: (T)-> Boolean): Boolean {
    return this != null && predicate(this)
}

/** Returns the 1 if the element is not null else 0  */
inline fun <T> T?.count(predicate: (T)-> Boolean): Int {
    return if (this != null) 1 else 0
}

/** Returns the first item which matches the predicate if this element is not null else null */
inline fun <T> T?.find(predicate: (T)-> Boolean): T? {
    return if (this != null && predicate(this)) this else null
}

/** Returns a new List containing all elements in this collection which match the given predicate */
inline fun <T> T?.filter(predicate: (T)-> Boolean): T? = find(predicate)

/** Filters all elements in this collection which match the given predicate into the given result collection */
inline fun <T, C: Collection<in T>> T?.filterTo(result: C, predicate: (T)-> Boolean): C {
    if (this != null && predicate(this))
        result.add(this)
    return result
}

/** Returns a List containing all the non null elements in this collection */
inline fun <T> T?.filterNotNull(): Collection<T> = filterNotNullTo(java.util.ArrayList<T>())

/** Filters all the null elements in this collection winto the given result collection */
inline fun <T, C: Collection<in T>> T?.filterNotNullTo(result: C): C {
    if (this != null) {
        result.add(this)
    }
    return result
}

/** Returns a new collection containing all elements in this collection which do not match the given predicate */
inline fun <T> T?.filterNot(predicate: (T)-> Boolean): Collection<T> = filterNotTo(ArrayList<T>(), predicate)

/** Returns a new collection containing all elements in this collection which do not match the given predicate */
inline fun <T, C: Collection<in T>> T?.filterNotTo(result: C, predicate: (T)-> Boolean): C {
    if (this != null && !predicate(this)) {
        result.add(this)
    }
    return result
}

/**
  * Returns the result of transforming each item in the collection to a one or more values which
  * are concatenated together into a single collection
  */
inline fun <T, R> T?.flatMap(transform: (T)-> Collection<R>): Collection<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
  * Returns the result of transforming each item in the collection to a one or more values which
  * are concatenated together into a single collection
  */
inline fun <T, R> T?.flatMapTo(result: Collection<R>, transform: (T)-> Collection<R>): Collection<R> {
    if (this != null) {
        val coll = transform(this)
        if (coll != null) {
            for (r in coll) {
                result.add(r)
            }
        }
    }
    return result
}

/** Performs the given operation on each element inside the collection */
inline fun <T> T?.forEach(operation: (element: T) -> Unit) {
    if (this != null) {
        operation(this)
    }
}

/**
 * Folds all the values from from left to right with the initial value to perform the operation on sequential pairs of values
 *
 * For example to sum together all numeric values in a collection of numbers it would be
 * {code}val total = numbers.fold(0){(a, b) -> a + b}{code}
 */
inline fun <T> T?.fold(initial: T, operation: (it: T, it2: T) -> T): T {
    return if (this != null) {
        operation(initial, this)
    } else {
        initial
    }
}

/**
 * Folds all the values from right to left with the initial value to perform the operation on sequential pairs of values
 */
inline fun <T> T?.foldRight(initial: T, operation: (it: T, it2: T) -> T): T {
    // maximum size is 1 so it makes no difference :)
    return fold(initial, operation)
}

/**
 * Iterates through the collection performing the transformation on each element and using the result
 * as the key in a map to group elements by the result
 */
inline fun <T, K> T?.groupBy(result: Map<K, List<T>> = HashMap<K, List<T>>(), toKey: (T)-> K): Map<K, List<T>> {
    if (this != null) {
        val key = toKey(this)
        val list = result.getOrPut(key){ ArrayList<T>() }
        list.add(this)
    }
    return result
}


/** Creates a String from the nullable or item with the given prefix and postfix if supplied */
inline fun <T> T?.join(separator: String, prefix: String = "", postfix: String = ""): String {
    val buffer = StringBuilder(prefix)
    var first = true
    if (this != null) {
        buffer.append(this)
    }
    buffer.append(postfix)
    return buffer.toString().sure()
}


/** Returns the nullable result of transforming this with the given transformation function */
inline fun <T, R> T?.map(transform : (T) -> R) : R? {
    return if (this != null) {
        transform(this)
    } else {
        null
    }
}

/** Transforms each element of this collection with the given function then adds the results to the given collection */
inline fun <T, R, C: Collection<in R>> T?.mapTo(result: C, transform : (T) -> R) : C {
    if (this != null) {
        result.add(transform(this))
    }
    return result
}

/** Returns itself since it can't be reversed as it can contain at most one item */
inline fun <T> T?.reverse(): T? {
    return this
}

/** Copies the collection into the given collection */
inline fun <T, C: Collection<T>> T?.to(result: C): C {
    if (this != null)
        result.add(this)
    return result
}

/** Converts the collection into a LinkedList */
inline fun <T> T?.toLinkedList(): LinkedList<T> = this.to(LinkedList<T>())

/**  Converts the collection into a List */
inline fun <T> T?.toList(): List<T> = this.to(ArrayList<T>())

/** Converts the collection into a Set */
inline fun <T> T?.toSet(): Set<T> = this.to(HashSet<T>())

/** Converts the collection into a SortedSet */
inline fun <T> T?.toSortedSet(): SortedSet<T> = this.to(TreeSet<T>())
/**
  TODO figure out necessary variance/generics ninja stuff... :)
inline fun <in T> T?.toSortedList(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
  val answer = this.toList()
  answer.sort(transform)
  return answer
}
*/
