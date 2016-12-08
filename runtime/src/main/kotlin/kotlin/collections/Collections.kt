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

fun <E> Array<E>.asList(): List<E> {
    // TODO: consider making lighter list over an array.
    val result = ArrayList<E>(this.size)
    for (e in this) {
        result.add(e)
    }
    return result
}

fun <E> Array<E>.toSet(): Set<E> {
    val result = HashSet<E>(this.size)
    for (e in this) {
        result.add(e)
    }
    return result
}

public fun <T> arrayListOf(vararg args: T): MutableList<T> {
    val result = ArrayList<T>(args.size)
    for (arg in args) {
        result.add(arg)
    }
    return result
}

public fun <T> listOf(): List<T> = EmptyList

public fun <T> listOf(vararg args: T): List<T> = args.asList()

public fun <T, C : MutableCollection</*in */T>> Iterable<T>.toCollection(destination: C): C {
    for (item in this) {
        destination.add(item)
    }
    return destination
}
