package kotlin

import java.util.*

private object EmptyList : List<Any> {
    private val list = ArrayList<Any>()

    override fun contains(o: Any?): Boolean = list.contains(o)
    override fun containsAll(c: Collection<Any?>): Boolean = list.containsAll(c)
    override fun get(index: Int): Any = list.get(index)
    override fun indexOf(o: Any?): Int = list.indexOf(o)
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun iterator(): Iterator<Any> = list.iterator()
    override fun lastIndexOf(o: Any?): Int = list.lastIndexOf(o)
    override fun listIterator(): ListIterator<Any> = list.listIterator()
    override fun listIterator(index: Int): ListIterator<Any> =list.listIterator(index)
    override fun size(): Int = list.size()
    override fun subList(fromIndex: Int, toIndex: Int): List<Any> = list.subList(fromIndex, toIndex)
    override fun equals(other: Any?): Boolean = list.equals(other)
    override fun hashCode(): Int = list.hashCode()
    override fun toString(): String = list.toString()
}

private object EmptySet : Set<Any> {
    private val set = HashSet<Any>()

    override fun contains(o: Any?): Boolean = set.contains(o)
    override fun containsAll(c: Collection<Any?>): Boolean = set.containsAll(c)
    override fun isEmpty(): Boolean = set.isEmpty()
    override fun iterator(): Iterator<Any> = set.iterator()
    override fun size(): Int = set.size()
    override fun equals(other: Any?): Boolean = set.equals(other)
    override fun hashCode(): Int = set.hashCode()
    override fun toString(): String = set.toString()
}

/** Returns an empty read-only list. */
public fun emptyList<T>(): List<T> = EmptyList as List<T>
/** Returns an empty read-only set. */
public fun emptySet<T>(): Set<T> = EmptySet as Set<T>

/** Returns a new read-only list of given elements */
public fun listOf<T>(vararg values: T): List<T> = if (values.size() == 0) emptyList() else arrayListOf(*values)

/** Returns an empty read-only list. */
public fun listOf<T>(): List<T> = emptyList()

/** Returns a new read-only ordered set with the given elements. */
public fun setOf<T>(vararg values: T): Set<T> = if (values.size() == 0) emptySet() else values.toCollection(LinkedHashSet<T>())

/** Returns an empty read-only set. */
public fun setOf<T>(): Set<T> = emptySet()

/** Returns a new [LinkedList] with the given elements. */
public fun linkedListOf<T>(vararg values: T): LinkedList<T> = values.toCollection(LinkedList<T>())

/** Returns a new [ArrayList] with the given elements. */
public fun arrayListOf<T>(vararg values: T): ArrayList<T> = values.toCollection(ArrayList(values.size()))

/** Returns a new [HashSet] with the given elements. */
public fun hashSetOf<T>(vararg values: T): HashSet<T> = values.toCollection(HashSet(values.size()))

/** Returns a new [LinkedHashSet] with the given elements. */
public fun linkedSetOf<T>(vararg values: T): LinkedHashSet<T> = values.toCollection(LinkedHashSet(values.size()))

/**
 * Returns an [IntRange] of the valid indices for this collection.
 */
public val Collection<*>.indices: IntRange
    get() = 0..size() - 1

/**
 * Returns an [IntRange] that starts with zero and ends at the value of this number but does not include it.
 */
public val Int.indices: IntRange
    get() = 0..this - 1

/**
 * Returns the index of the last item in the list or -1 if the list is empty
 *
 * @sample test.collections.ListSpecificTest.lastIndex
 */
public val <T> List<T>.lastIndex: Int
    get() = this.size() - 1

/** Returns true if the collection is not empty */
public fun <T> Collection<T>.isNotEmpty(): Boolean = !isEmpty()

/** Returns this Collection if it's not null and the empty list otherwise. */
public fun <T> Collection<T>?.orEmpty(): Collection<T> = this ?: emptyList()

/** Returns this List if it's not null and the empty list otherwise. */
public fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()

/** Returns this Set if it's not null and the empty set otherwise. */
public fun <T> Set<T>?.orEmpty(): Set<T> = this ?: emptySet()

/**
 * Returns the size of this iterable if it is known, or `null` otherwise.
 */
public fun <T> Iterable<T>.collectionSizeOrNull(): Int? = if (this is Collection<*>) size() else null

/**
 * Returns the size of this iterable if it is known, or the specified [default] value otherwise.
 */
public fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int = if (this is Collection<*>) size() else default