package kotlin

import java.io.Serializable
import java.util.*

private object EmptyIterator : ListIterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun hasPrevious(): Boolean = false
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = -1
    override fun next(): Nothing = throw NoSuchElementException()
    override fun previous(): Nothing = throw NoSuchElementException()
}

private object EmptyList : List<Nothing>, Serializable {
    override fun equals(other: Any?): Boolean = other is List<*> && other.isEmpty()
    override fun hashCode(): Int = 1
    override fun toString(): String = "[]"

    override fun size(): Int = 0
    override fun isEmpty(): Boolean = true
    override fun contains(o: Any?): Boolean = false
    override fun containsAll(c: Collection<Any?>): Boolean = c.isEmpty()

    override fun get(index: Int): Nothing = throw IndexOutOfBoundsException("Index $index is out of bound of empty list.")
    override fun indexOf(o: Any?): Int = -1
    override fun lastIndexOf(o: Any?): Int = -1

    override fun iterator(): Iterator<Nothing> = EmptyIterator
    override fun listIterator(): ListIterator<Nothing> = EmptyIterator
    override fun listIterator(index: Int): ListIterator<Nothing> {
        if (index != 0) throw IndexOutOfBoundsException("Index: $index")
        return EmptyIterator
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Nothing> {
        if (fromIndex == 0 && toIndex == 0) return this
        throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex")
    }

    private fun readResolve(): Any = EmptyList
}

private object EmptySet : Set<Nothing>, Serializable {
    override fun equals(other: Any?): Boolean = other is Set<*> && other.isEmpty()
    override fun hashCode(): Int = 0
    override fun toString(): String = "[]"

    override fun size(): Int = 0
    override fun isEmpty(): Boolean = true
    override fun contains(o: Any?): Boolean = false
    override fun containsAll(c: Collection<Any?>): Boolean = c.isEmpty()

    override fun iterator(): Iterator<Nothing> = EmptyIterator

    private fun readResolve(): Any = EmptySet
}

/** Returns an empty read-only list.  The returned list is serializable (JVM). */
public fun emptyList<T>(): List<T> = EmptyList
/** Returns an empty read-only set.  The returned set is serializable (JVM). */
public fun emptySet<T>(): Set<T> = EmptySet

/** Returns a new read-only list of given elements.  The returned list is serializable (JVM). */
public fun listOf<T>(vararg values: T): List<T> = if (values.size() > 0) arrayListOf(*values) else emptyList()

/** Returns an empty read-only list.  The returned list is serializable (JVM). */
public fun listOf<T>(): List<T> = emptyList()

/** Returns a new read-only ordered set with the given elements.  The returned set is serializable (JVM). */
public fun setOf<T>(vararg values: T): Set<T> = if (values.size() > 0) values.toSet() else emptySet()

/** Returns an empty read-only set.  The returned set is serializable (JVM). */
public fun setOf<T>(): Set<T> = emptySet()

/** Returns a new [LinkedList] with the given elements. */
public fun linkedListOf<T>(vararg values: T): LinkedList<T> = values.toCollection(LinkedList<T>())

/** Returns a new [ArrayList] with the given elements. */
public fun arrayListOf<T>(vararg values: T): ArrayList<T> = values.toCollection(ArrayList(values.size()))

/** Returns a new [HashSet] with the given elements. */
public fun hashSetOf<T>(vararg values: T): HashSet<T> = values.toCollection(HashSet(mapCapacity(values.size())))

/** Returns a new [LinkedHashSet] with the given elements. */
public fun linkedSetOf<T>(vararg values: T): LinkedHashSet<T> = values.toCollection(LinkedHashSet(mapCapacity(values.size())))

/**
 * Returns an [IntRange] of the valid indices for this collection.
 */
public val Collection<*>.indices: IntRange
    get() = 0..size() - 1

/**
 * Returns an [IntRange] that starts with zero and ends at the value of this number but does not include it.
 */
deprecated("Use 0..n-1 range instead.", ReplaceWith("0..this - 1"))
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