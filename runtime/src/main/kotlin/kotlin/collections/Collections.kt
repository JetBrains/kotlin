package kotlin.collections

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

public fun <T> arrayListOf(vararg args: T): MutableList<T> {
    val result = ArrayList<T>(args.size)
    for (arg in args) {
        result.add(arg)
    }
    return result
}

/*
 * TODO: in Big Kotlin this function is following: (see libraries/stdlib/src/kotlin/collections/Collections.kt)
 * public fun <T> listOf(vararg elements: T): List<T> = if (elements.size > 0) elements.asList() else emptyList()
 */
public fun <T> listOf(vararg args: T): List<T> = args.asList()