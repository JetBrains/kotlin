package kotlin

import java.util.*

@Deprecated("Use listOf(...) or arrayListOf(...) instead", ReplaceWith("arrayListOf(*values)"))
public fun arrayList<T>(vararg values: T): ArrayList<T> = arrayListOf(*values)

@Deprecated("Use setOf(...) or hashSetOf(...) instead", ReplaceWith("hashSetOf(*values)"))
public fun hashSet<T>(vararg values: T): HashSet<T> = hashSetOf(*values)

@Deprecated("Use mapOf(...) or hashMapOf(...) instead", ReplaceWith("hashMapOf(*values)"))
public fun <K, V> hashMap(vararg values: Pair<K, V>): HashMap<K, V> = hashMapOf(*values)

@Deprecated("Use listOf(...) or linkedListOf(...) instead", ReplaceWith("linkedListOf(*values)"))
public fun linkedList<T>(vararg values: T): LinkedList<T> = linkedListOf(*values)

@Deprecated("Use linkedMapOf(...) instead", ReplaceWith("linkedMapOf(*values)"))
public fun <K, V> linkedMap(vararg values: Pair<K, V>): LinkedHashMap<K, V> = linkedMapOf(*values)

/** Copies all characters into a [[Collection] */
@Deprecated("Use toList() instead.", ReplaceWith("toList()"))
public fun String.toCollection(): Collection<Char> = toCollection(ArrayList<Char>(this.length()))


/**
 * A helper method for creating a [[Runnable]] from a function
 */
@Deprecated("Use SAM constructor: Runnable(...)", ReplaceWith("Runnable(action)"))
public /*inline*/ fun runnable(action: () -> Unit): Runnable = Runnable(action)

@Deprecated("Use forEachIndexed instead.", ReplaceWith("forEachIndexed(operation)"))
public inline fun <T> List<T>.forEachWithIndex(operation: (Int, T) -> Unit): Unit = forEachIndexed(operation)

@Deprecated("Function with undefined semantic")
public fun <T> countTo(n: Int): (T) -> Boolean {
    var count = 0
    return { ++count; count <= n }
}

@Deprecated("Use contains() function instead", ReplaceWith("contains(item)"))
public fun <T> Iterable<T>.containsItem(item : T) : Boolean = contains(item)

@Deprecated("Use sortBy() instead", ReplaceWith("sortedWith(comparator)"))
public fun <T> Iterable<T>.sort(comparator: java.util.Comparator<T>) : List<T> = sortedWith(comparator)

@Deprecated("Use size() instead", ReplaceWith("size()"))
public val Array<*>.size: Int get() = size()

@Deprecated("Use size() instead", ReplaceWith("size()"))
public val ByteArray.size: Int get() = size()

@Deprecated("Use size() instead", ReplaceWith("size()"))
public val CharArray.size: Int get() = size()

@Deprecated("Use size() instead", ReplaceWith("size()"))
public val ShortArray.size: Int get() = size()

@Deprecated("Use size() instead", ReplaceWith("size()"))
public val IntArray.size: Int get() = size()

@Deprecated("Use size() instead", ReplaceWith("size()"))
public val LongArray.size: Int get() = size()

@Deprecated("Use size() instead", ReplaceWith("size()"))
public val FloatArray.size: Int get() = size()

@Deprecated("Use size() instead", ReplaceWith("size()"))
public val DoubleArray.size: Int get() = size()

@Deprecated("Use size() instead", ReplaceWith("size()"))
public val BooleanArray.size: Int get() = size()

@Deprecated("Use compareValuesBy() instead", ReplaceWith("compareValuesBy(a, b, *functions)"))
public fun <T : Any> compareBy(a: T?, b: T?, vararg functions: (T) -> Comparable<*>?): Int = compareValuesBy(a, b, *functions)


/** Returns true if this collection is empty */
@Deprecated("Use isEmpty() function call instead", ReplaceWith("isEmpty()"))
public val Collection<*>.empty: Boolean
    get() = isEmpty()

/** Returns the size of the collection */
@Deprecated("Use size() function call instead", ReplaceWith("size()"))
public val Collection<*>.size: Int
    get() = size()


/** Returns the size of the map */
@Deprecated("Use size() function call instead", ReplaceWith("size()"))
public val Map<*, *>.size: Int
    get() = size()

/** Returns true if this map is empty */
@Deprecated("Use isEmpty() function call instead", ReplaceWith("isEmpty()"))
public val Map<*, *>.empty: Boolean
    get() = isEmpty()

/** Returns true if this collection is not empty */
@Deprecated("Use isNotEmpty() function call instead", ReplaceWith("isNotEmpty()"))
public val Collection<*>.notEmpty: Boolean
    get() = isNotEmpty()

@Deprecated("Use length() instead", ReplaceWith("length()"))
public val CharSequence.length: Int
    get() = length()

