package kotlin

import java.util.*
import java.util.concurrent.Callable

deprecated("Use firstOrNull function instead.")
public inline fun <T> Array<out T>.find(predicate: (T) -> Boolean): T? = firstOrNull(predicate)

deprecated("Use firstOrNull function instead.")
public inline fun <T> Iterable<T>.find(predicate: (T) -> Boolean): T? = firstOrNull(predicate)

deprecated("Use listOf(...) or arrayListOf(...) instead")
public fun arrayList<T>(vararg values: T): ArrayList<T> = arrayListOf(*values)

deprecated("Use setOf(...) or hashSetOf(...) instead")
public fun hashSet<T>(vararg values: T): HashSet<T> = hashSetOf(*values)

deprecated("Use mapOf(...) or hashMapOf(...) instead")
public fun <K, V> hashMap(vararg values: Pair<K, V>): HashMap<K, V> = hashMapOf(*values)

deprecated("Use listOf(...) or linkedListOf(...) instead")
public fun linkedList<T>(vararg values: T): LinkedList<T> = linkedListOf(*values)

deprecated("Use sortedSetOf(...) instead")
public fun sortedSet<T>(vararg values: T): TreeSet<T> = sortedSetOf(*values)

deprecated("Use sortedSetOf(...) instead")
public fun sortedSet<T>(comparator: Comparator<T>, vararg values: T): TreeSet<T> = sortedSetOf(comparator, *values)

deprecated("Use sortedMapOf(...) instead")
public fun <K, V> sortedMap(vararg values: Pair<K, V>): SortedMap<K, V> = sortedMapOf(*values)

deprecated("Use linkedMapOf(...) instead")
public fun <K, V> linkedMap(vararg values: Pair<K, V>): LinkedHashMap<K, V> = linkedMapOf(*values)

/**
 * A helper method for creating a [[Callable]] from a function
 */
deprecated("Use SAM constructor: Callable(...)")
public /*inline*/ fun <T> callable(action: () -> T): Callable<T> {
    return object: Callable<T> {
        public override fun call() = action()
    }
}

/**
 * A helper method for creating a [[Runnable]] from a function
 */
deprecated("Use SAM constructor: Runnable(...)")
public /*inline*/ fun runnable(action: () -> Unit): Runnable {
    return object: Runnable {
        public override fun run() {
            action()
        }
    }
}

deprecated("Use withIndices() followed by forEach {}")
public inline fun <T> List<T>.forEachWithIndex(operation : (Int, T) -> Unit): Unit =  withIndices().forEach {
    operation(it.first, it.second)
}

deprecated("Function with undefined semantic")
public fun <T> countTo(n: Int): (T) -> Boolean {
    var count = 0
    return { ++count; count <= n }
}

deprecated("Use contains() function instead")
public fun <T> Iterable<T>.containsItem(item : T) : Boolean = contains(item)

deprecated("Use sortBy() instead")
public fun <T> Iterable<T>.sort(comparator: java.util.Comparator<T>) : List<T> = sortBy(comparator)
