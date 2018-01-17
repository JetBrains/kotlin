@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SetsKt")

package kotlin.collections


internal object EmptySet : Set<Nothing>, Serializable {
    private const val serialVersionUID: Long = 3406603774387020532

    override fun equals(other: Any?): Boolean = other is Set<*> && other.isEmpty()
    override fun hashCode(): Int = 0
    override fun toString(): String = "[]"

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()

    override fun iterator(): Iterator<Nothing> = EmptyIterator

    private fun readResolve(): Any = EmptySet
}


/**
 * Returns an empty read-only set.  The returned set is serializable (JVM).
 * @sample samples.collections.Collections.Sets.emptyReadOnlySet
 */
public fun <T> emptySet(): Set<T> = EmptySet
/**
 * Returns a new read-only set with the given elements.
 * Elements of the set are iterated in the order they were specified.
 * The returned set is serializable (JVM).
 */
public fun <T> setOf(vararg elements: T): Set<T> = if (elements.size > 0) elements.toSet() else emptySet()

/**
 * Returns an empty read-only set.  The returned set is serializable (JVM).
 * @sample samples.collections.Collections.Sets.emptyReadOnlySet
 */
@kotlin.internal.InlineOnly
public inline fun <T> setOf(): Set<T> = emptySet()

/**
 * Returns an empty new [MutableSet].
 *
 * The returned set preserves the element iteration order.
 */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> mutableSetOf(): MutableSet<T> = LinkedHashSet()

/**
 * Returns a new [MutableSet] with the given elements.
 * Elements of the set are iterated in the order they were specified.
 */
public fun <T> mutableSetOf(vararg elements: T): MutableSet<T> = elements.toCollection(LinkedHashSet(mapCapacity(elements.size)))

/** Returns an empty new [HashSet]. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> hashSetOf(): HashSet<T> = HashSet()

/** Returns a new [HashSet] with the given elements. */
public fun <T> hashSetOf(vararg elements: T): HashSet<T> = elements.toCollection(HashSet(mapCapacity(elements.size)))

/** Returns an empty new [LinkedHashSet]. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun <T> linkedSetOf(): LinkedHashSet<T> = LinkedHashSet()

/**
 * Returns a new [LinkedHashSet] with the given elements.
 * Elements of the set are iterated in the order they were specified.
 */
public fun <T> linkedSetOf(vararg elements: T): LinkedHashSet<T> = elements.toCollection(LinkedHashSet(mapCapacity(elements.size)))

/** Returns this Set if it's not `null` and the empty set otherwise. */
@kotlin.internal.InlineOnly
public inline fun <T> Set<T>?.orEmpty(): Set<T> = this ?: emptySet()

/**
 * Returns an immutable set containing only the specified object [element].
 * The returned set is serializable.
 */
@JvmVersion
public fun <T> setOf(element: T): Set<T> = java.util.Collections.singleton(element)


/**
 * Returns a new [SortedSet] with the given elements.
 */
@JvmVersion
public fun <T> sortedSetOf(vararg elements: T): TreeSet<T> = elements.toCollection(TreeSet<T>())

/**
 * Returns a new [SortedSet] with the given [comparator] and elements.
 */
@JvmVersion
public fun <T> sortedSetOf(comparator: Comparator<in T>, vararg elements: T): TreeSet<T> = elements.toCollection(TreeSet<T>(comparator))


internal fun <T> Set<T>.optimizeReadOnlySet() = when (size) {
    0 -> emptySet()
    1 -> setOf(iterator().next())
    else -> this
}
