package kotlin.nullable

import java.util.*

/** Returns true if the element is not null and matches the given predicate */
public inline fun <T: Any> T?.any(predicate: (T)-> Boolean): Boolean {
    return this != null && predicate(this)
}

/** Returns true if the element is not null and matches the given predicate */
public inline fun <T: Any> T?.all(predicate: (T)-> Boolean): Boolean {
    return this != null && predicate(this)
}

/** Returns the 1 if the element is not null else 0  */
public inline fun <T: Any> T?.count(predicate: (T)-> Boolean): Int {
    return if (this != null) 1 else 0
}

/** Returns the first item which matches the predicate if this element is not null else null */
public inline fun <T: Any> T?.find(predicate: (T)-> Boolean): T? {
    return if (this != null && predicate(this)) this else null
}

/** Returns a new List containing all elements in this collection which match the given predicate */
public inline fun <T: Any> T?.filter(predicate: (T)-> Boolean): T? = find(predicate)

/** Filters all elements in this collection which match the given predicate into the given result collection */
public inline fun <T: Any, C: MutableCollection<in T>> T?.filterTo(result: C, predicate: (T)-> Boolean): C {
    if (this != null && predicate(this))
        result.add(this)
    return result
}

/** Returns a List containing all the non null elements in this collection */
public inline fun <T: Any> T?.filterNotNull(): Collection<T> = filterNotNullTo(java.util.ArrayList<T>())

/** Filters all the null elements in this collection winto the given result collection */
public inline fun <T: Any, C: MutableCollection<in T>> T?.filterNotNullTo(result: C): C {
    if (this != null) {
        result.add(this)
    }
    return result
}

/** Returns a new collection containing all elements in this collection which do not match the given predicate */
public inline fun <T: Any> T?.filterNot(predicate: (T)-> Boolean): Collection<T> = filterNotTo(ArrayList<T>(), predicate)

/** Returns a new collection containing all elements in this collection which do not match the given predicate */
public inline fun <T: Any, C: MutableCollection<in T>> T?.filterNotTo(result: C, predicate: (T)-> Boolean): C {
    if (this != null && !predicate(this)) {
        result.add(this)
    }
    return result
}

/**
  * Returns the result of transforming each item in the collection to a one or more values which
  * are concatenated together into a single collection
  */
public inline fun <T: Any, R> T?.flatMap(transform: (T)-> MutableCollection<R>): Collection<R> {
    return flatMapTo(ArrayList<R>(), transform)
}

/**
  * Returns the result of transforming each item in the collection to a one or more values which
  * are concatenated together into a single collection
  */
public inline fun <T: Any, R> T?.flatMapTo(result: MutableCollection<R>, transform: (T)-> MutableCollection<R>): Collection<R> {
    if (this != null) {
        val coll = transform(this)
        for (r in coll) {
            result.add(r)
        }
    }
    return result
}

/** Performs the given operation on each element inside the collection */
public inline fun <T: Any> T?.forEach(operation: (element: T) -> Unit) {
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
public inline fun <T: Any> T?.fold(initial: T, operation: (it: T, it2: T) -> T): T {
    return if (this != null) {
        operation(initial, this)
    } else {
        initial
    }
}

/**
 * Folds all the values from right to left with the initial value to perform the operation on sequential pairs of values
 */
public inline fun <T: Any> T?.foldRight(initial: T, operation: (it: T, it2: T) -> T): T {
    // maximum size is 1 so reverse is not needed
    return fold(initial, {x, y -> operation(y, x)})
}

/**
 * Returns the value of the Nullable unless it is null, otherwise it will return the value passed in.  Equivalent to ?:
 * but supports further chaining.
 */
public inline fun <T> T?.getOrElse(alt : T) : T {
    return this ?: alt
}

/**
 * Returns the value of the Nullable unless it is null, otherwise it will return the result of the expression passed in.  
 * Equivalent to ?: but supports further chaining.
 */
public inline fun <T> T?.getOrElse(t: () -> T) : T {
    return this ?: t()
}

/**
 * Iterates through the collection performing the transformation on each element and using the result
 * as the key in a map to group elements by the result
 */
public inline fun <T: Any, K> T?.groupBy(result: MutableMap<K, MutableList<T>> = HashMap<K, MutableList<T>>(), toKey: (T)-> K): Map<K, MutableList<T>> {
    if (this != null) {
        val key = toKey(this)
        val list = result.getOrPut(key){ ArrayList<T>() }
        list.add(this)
    }
    return result
}


/** Creates a String from the nullable or item with the given prefix and postfix if supplied */
public inline fun <T: Any> T?.makeString(separator: String = ", ", prefix: String = "", postfix: String = ""): String {
    val buffer = StringBuilder(prefix)
    if (this != null) {
        buffer.append(this)
    }
    buffer.append(postfix)
    return buffer.toString()
}


/** Returns the nullable result of transforming this with the given transformation function */
public inline fun <T: Any, R> T?.map(transform : (T) -> R) : R? {
    return if (this != null) {
        transform(this)
    } else {
        null
    }
}

/** Transforms each element of this collection with the given function then adds the results to the given collection */
public inline fun <T: Any, R, C: MutableCollection<in R>> T?.mapTo(result: C, transform : (T) -> R) : C {
    if (this != null) {
        result.add(transform(this))
    }
    return result
}

/** Returns itself since it can't be reversed as it can contain at most one item */
public inline fun <T: Any> T?.reverse(): T? {
    return this
}

/** Copies the collection into the given collection */
public inline fun <T: Any, C: MutableCollection<T>> T?.toCollection(result: C): C {
    if (this != null)
        result.add(this)
    return result
}

/** Converts the collection into a LinkedList */
public inline fun <T: Any> T?.toLinkedList(): LinkedList<T> = this.toCollection(LinkedList<T>())

/**  Converts the collection into a List */
public inline fun <T: Any> T?.toList(): List<T> = this.toCollection(ArrayList<T>())

/** Converts the collection into a Set */
public inline fun <T: Any> T?.toSet(): Set<T> = this.toCollection(HashSet<T>())

/** Converts the collection into a SortedSet */
public inline fun <T: Any> T?.toSortedSet(): SortedSet<T> = this.toCollection(TreeSet<T>())
/**
  TODO figure out necessary variance/generics ninja stuff... :)
public inline fun <in T> T?.toSortedList(transform: fun(T) : java.lang.Comparable<*>) : List<T> {
  val answer = this.toList()
  answer.sort(transform)
  return answer
}
*/


/** Returns a hash code on an existing object, or default value otherwise */
 public inline fun Any?.hashCodeOrDefault(default: Int): Int = if (this == null) default else this.hashCode()
