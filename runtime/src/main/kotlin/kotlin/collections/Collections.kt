package kotlin.collections

internal object EmptyIterator : ListIterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun hasPrevious(): Boolean = false
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = -1
    override fun next(): Nothing = throw NoSuchElementException()
    override fun previous(): Nothing = throw NoSuchElementException()
}

internal object EmptyList : List<Nothing>/*, RandomAccess */ {

    override fun equals(other: Any?): Boolean = other is List<*> && other.isEmpty()
    override fun hashCode(): Int = 1
    override fun toString(): String = "[]"

    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()

    override fun get(index: Int): Nothing = throw IndexOutOfBoundsException("Empty list doesn't contain element at index $index.")
    override fun indexOf(element: Nothing): Int = -1
    override fun lastIndexOf(element: Nothing): Int = -1

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

internal fun <T> Array<out T>.asCollection(): Collection<T> = ArrayAsCollection(this, isVarargs = false)

private class ArrayAsCollection<T>(val values: Array<out T>, val isVarargs: Boolean): Collection<T> {
    override val size: Int get() = values.size
    override fun isEmpty(): Boolean = values.isEmpty()
    override fun contains(element: T): Boolean = values.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }
    override fun iterator(): Iterator<T> = values.iterator()
    // override hidden toArray implementation to prevent copying of values array
    public fun toArray(): Array<out Any?> = values.copyToArrayOfAny(isVarargs)
}

/** Returns an empty read-only list.  */
public fun <T> emptyList(): List<T> = EmptyList

/** Returns a new read-only list of given elements. */
public fun <T> listOf(vararg elements: T): List<T> = if (elements.size > 0) elements.asList() else emptyList()

/** Returns an empty read-only list. */
@kotlin.internal.InlineOnly
public inline fun <T> listOf(): List<T> = emptyList()

/** Returns a new [MutableList] with the given elements. */
public fun <T> mutableListOf(vararg elements: T): MutableList<T>
        = if (elements.size == 0) ArrayList() else ArrayList(ArrayAsCollection(elements, isVarargs = true))

// This part is from generated _Collections.kt.

/** Returns a new [ArrayList] with the given elements. */
public fun <T> arrayListOf(vararg elements: T): ArrayList<T>
        = if (elements.size == 0) ArrayList() else ArrayList(ArrayAsCollection(elements, isVarargs = true))

/** Returns a new read-only list either of single given element, if it is not null,
 * or empty list it the element is null.*/
public fun <T : Any> listOfNotNull(element: T?): List<T> =
        if (element != null) listOf(element) else emptyList()

/** Returns a new read-only list only of those given elements, that are not null. */
public fun <T : Any> listOfNotNull(vararg elements: T?): List<T> = elements.filterNotNull()

/**
 * Returns an [IntRange] of the valid indices for this collection.
 */
public val Collection<*>.indices: IntRange
    get() = 0..size - 1

/**
 * Returns the index of the last item in the list or -1 if the list is empty.
 *
 * @sample samples.collections.Collections.Lists.lastIndexOfList
 */
public val <T> List<T>.lastIndex: Int
    get() = this.size - 1

/** Returns `true` if the collection is not empty. */
@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>.isNotEmpty(): Boolean = !isEmpty()

/** Returns this Collection if it's not `null` and the empty list otherwise. */
@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>?.orEmpty(): Collection<T> = this ?: emptyList()

@kotlin.internal.InlineOnly
public inline fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()

/**
 * Checks if all elements in the specified collection are contained in this collection.
 *
 * Allows to overcome type-safety restriction of `containsAll` that requires to pass a collection of type `Collection<E>`.
 */
@kotlin.internal.InlineOnly
public inline fun <@kotlin.internal.OnlyInputTypes T> Collection<T>.containsAll(
        elements: Collection<T>): Boolean = this.containsAll(elements)

// copies typed varargs array to array of objects
// TODO: generally wrong, wrt specialization.
@Fixme
private fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<Any?> =
        if (isVarargs)
            // if the array came from varargs and already is array of Any, copying isn't required.
            @Suppress("UNCHECKED_CAST") (this as Array<Any?>)
        else
            @Suppress("UNCHECKED_CAST") (this.copyOfUninitializedElements(this.size) as Array<Any?>)


/**
 * Classes that inherit from this interface can be represented as a sequence of elements that can
 * be iterated over.
 * @param T the type of element being iterated over.
 */
public interface Iterable<out T> {
    /**
     * Returns an iterator over the elements of this object.
     */
    public operator fun iterator(): Iterator<T>
}

/**
 * Classes that inherit from this interface can be represented as a sequence of elements that can
 * be iterated over and that supports removing elements during iteration.
 */
public interface MutableIterable<out T> : Iterable<T> {
    /**
     * Returns an iterator over the elementrs of this sequence that supports removing elements during iteration.
     */
    override fun iterator(): MutableIterator<T>
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun <T> Iterable<T>.all(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if collection has at least one element.
 */
public fun <T> Iterable<T>.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun <T> Iterable<T>.any(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun <T> Iterable<T>.filterNot(predicate: (T) -> Boolean): List<T> {
    return filterNotTo(ArrayList<T>(), predicate)
}

/**
 * Returns a list containing all elements that are not `null`.
 */
public fun <T : Any> Iterable<T?>.filterNotNull(): List<T> {
    return filterNotNullTo(ArrayList<T>())
}

/**
 * Appends all elements that are not `null` to the given [destination].
 */
public fun <C : MutableCollection<in T>, T : Any> Iterable<T?>.filterNotNullTo(destination: C): C {
    for (element in this) if (element != null) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <T, C : MutableCollection<in T>> Iterable<T>.filterNotTo(destination: C, predicate: (T) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}


fun <E> Array<E>.asList(): List<E> {
    // TODO: consider making lighter list over an array.
    val result = ArrayList<E>(this.size)
    for (e in this) {
        result.add(e)
    }
    return result
}
/* TODO: use this one!
public fun <T> Array<out T>.asList(): List<T> {
    return object : AbstractList<T>() {
        override val size: Int get() = this@asList.size
        override fun isEmpty(): Boolean = this@asList.isEmpty()
        override fun contains(element: T): Boolean = this@asList.contains(element)
        override fun get(index: Int): T = this@asList[index]
        override fun indexOf(element: T): Int = this@asList.indexOf(element)
        override fun lastIndexOf(element: T): Int = this@asList.lastIndexOf(element)
    }
} */

fun <E> Array<E>.toSet(): Set<E> {
    val result = HashSet<E>(this.size)
    for (e in this) {
        result.add(e)
    }
    return result
}

public fun <T, C : MutableCollection</*in */T>> Iterable<T>.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}

// From generated _Collections.kt.
/**
 * Returns index of the first element matching the given [predicate], or -1 if the collection does not contain such element.
 */
public inline fun <T> Iterable<T>.indexOfFirst(predicate: (T) -> Boolean): Int {
    var index = 0
    for (item in this) {
        if (predicate(item))
            return index
        index++
    }
    return -1
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the list does not contain such element.
 */
public inline fun <T> List<T>.indexOfFirst(predicate: (T) -> Boolean): Int {
    var index = 0
    for (item in this) {
        if (predicate(item))
            return index
        index++
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the collection does not contain such element.
 */
public inline fun <T> Iterable<T>.indexOfLast(predicate: (T) -> Boolean): Int {
    var lastIndex = -1
    var index = 0
    for (item in this) {
        if (predicate(item))
            lastIndex = index
        index++
    }
    return lastIndex
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the list does not contain such element.
 */
public inline fun <T> List<T>.indexOfLast(predicate: (T) -> Boolean): Int {
    val iterator = this.listIterator(size)
    while (iterator.hasPrevious()) {
        if (predicate(iterator.previous())) {
            return iterator.nextIndex()
        }
    }
    return -1
}
