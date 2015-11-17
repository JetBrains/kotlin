@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SetsKt")

package kotlin

import java.io.Serializable
import java.util.*


internal object EmptySet : Set<Nothing>, Serializable {
    override fun equals(other: Any?): Boolean = other is Set<*> && other.isEmpty()
    override fun hashCode(): Int = 0
    override fun toString(): String = "[]"

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(o: Nothing): Boolean = false
    override fun containsAll(c: Collection<Nothing>): Boolean = c.isEmpty()

    override fun iterator(): Iterator<Nothing> = EmptyIterator

    private fun readResolve(): Any = EmptySet
}


/** Returns an empty read-only set.  The returned set is serializable (JVM). */
public fun <T> emptySet(): Set<T> = EmptySet
/** Returns a new read-only ordered set with the given elements.  The returned set is serializable (JVM). */
public fun <T> setOf(vararg values: T): Set<T> = if (values.size > 0) values.toSet() else emptySet()

/** Returns an empty read-only set.  The returned set is serializable (JVM). */
public fun <T> setOf(): Set<T> = emptySet()


/** Returns a new [HashSet] with the given elements. */
public fun <T> hashSetOf(vararg values: T): HashSet<T> = values.toCollection(HashSet(mapCapacity(values.size)))

/** Returns a new [LinkedHashSet] with the given elements. */
public fun <T> linkedSetOf(vararg values: T): LinkedHashSet<T> = values.toCollection(LinkedHashSet(mapCapacity(values.size)))

/** Returns this Set if it's not `null` and the empty set otherwise. */
public fun <T> Set<T>?.orEmpty(): Set<T> = this ?: emptySet()

/**
 * Returns an immutable set containing only the specified object [value].
 * The returned set is serializable.
 */
@JvmVersion
public fun <T> setOf(value: T): Set<T> = Collections.singleton(value)


/**
 * Returns a new [SortedSet] with the given elements.
 */
@JvmVersion
public fun <T> sortedSetOf(vararg values: T): TreeSet<T> = values.toCollection(TreeSet<T>())

/**
 * Returns a new [SortedSet] with the given [comparator] and elements.
 */
@JvmVersion
public fun <T> sortedSetOf(comparator: Comparator<in T>, vararg values: T): TreeSet<T> = values.toCollection(TreeSet<T>(comparator))
