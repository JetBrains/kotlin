package kotlin

import java.util.*

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

deprecated("Use linkedMapOf(...) instead")
public fun <K, V> linkedMap(vararg values: Pair<K, V>): LinkedHashMap<K, V> = linkedMapOf(*values)

/** Copies all characters into a [[Collection] */
deprecated("Use toList() instead.")
public fun String.toCollection(): Collection<Char> = toCollection(ArrayList<Char>(this.length()))

/**
 * Returns the first character which matches the given *predicate* or *null* if none matched
 *
 * @includeFunctionBody ../../test/text/StringTest.kt find
 */
deprecated("Use firstOrNull instead")
public inline fun String.find(predicate: (Char) -> Boolean): Char? {
    for (c in this) if (predicate(c)) return c
    return null
}

/**
 * Returns the first character which does not match the given *predicate* or *null* if none matched
 *
 * @includeFunctionBody ../../test/text/StringTest.kt findNot
 */
deprecated("Use firstOrNull instead")
public inline fun String.findNot(predicate: (Char) -> Boolean): Char? {
    for (c in this) if (!predicate(c)) return c
    return null
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

deprecated("Use forEachIndexed instead.")
public inline fun <T> List<T>.forEachWithIndex(operation: (Int, T) -> Unit): Unit = forEachIndexed(operation)

deprecated("Function with undefined semantic")
public fun <T> countTo(n: Int): (T) -> Boolean {
    var count = 0
    return { ++count; count <= n }
}

deprecated("Use contains() function instead")
public fun <T> Iterable<T>.containsItem(item : T) : Boolean = contains(item)

deprecated("Use sortBy() instead")
public fun <T> Iterable<T>.sort(comparator: java.util.Comparator<T>) : List<T> = sortBy(comparator)

deprecated("Use size() instead")
public val Array<*>.size: Int get() = size()

deprecated("Use size() instead")
public val ByteArray.size: Int get() = size()

deprecated("Use size() instead")
public val CharArray.size: Int get() = size()

deprecated("Use size() instead")
public val ShortArray.size: Int get() = size()

deprecated("Use size() instead")
public val IntArray.size: Int get() = size()

deprecated("Use size() instead")
public val LongArray.size: Int get() = size()

deprecated("Use size() instead")
public val FloatArray.size: Int get() = size()

deprecated("Use size() instead")
public val DoubleArray.size: Int get() = size()

deprecated("Use size() instead")
public val BooleanArray.size: Int get() = size()

deprecated("Use compareValuesBy() instead")
public fun <T : Any> compareBy(a: T?, b: T?, vararg functions: (T) -> Comparable<*>?): Int = compareValuesBy(a, b, *functions)


/**
 * Returns the first item in the list or null if the list is empty
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt first
 */
deprecated("Use firstOrNull() function instead")
public val <T> List<T>.first: T?
    get() = this.firstOrNull()


/**
 * Returns the last item in the list or null if the list is empty
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt last
 */
deprecated("Use lastOrNull() function instead")
public val <T> List<T>.last: T?
    get() {
        val s = this.size()
        return if (s > 0) this[s - 1] else null
    }


/**
 * Returns the first item in the list or null if the list is empty
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt head
 */
deprecated("Use firstOrNull() function instead")
public val <T> List<T>.head: T?
    get() = firstOrNull()

/**
 * Returns all elements in this collection apart from the first one
 *
 * @includeFunctionBody ../../test/collections/ListSpecificTest.kt tail
 */
deprecated("Use drop(1) function call instead")
public val <T> List<T>.tail: List<T>
    get() {
        return drop(1)
    }

/** Returns true if this collection is empty */
deprecated("Use isEmpty() function call instead")
public val Collection<*>.empty: Boolean
    get() = isEmpty()

/** Returns the size of the collection */
deprecated("Use size() function call instead")
public val Collection<*>.size: Int
    get() = size()


/** Returns the size of the map */
deprecated("Use size() function call instead")
public val Map<*, *>.size: Int
    get() = size()

/** Returns true if this map is empty */
deprecated("Use isEmpty() function call instead")
public val Map<*, *>.empty: Boolean
    get() = isEmpty()

/** Returns true if this collection is not empty */
deprecated("Use isNotEmpty() function call instead")
public val Collection<*>.notEmpty: Boolean
    get() = isNotEmpty()

deprecated("Use length() instead")
public val CharSequence.length: Int
    get() = length()

