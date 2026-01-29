package kotlin.collections

public interface Iterable<out T> {
    public operator fun iterator(): Iterator<T>
}

public interface MutableIterable<out T> : Iterable<T> {
    public override operator fun iterator(): MutableIterator<T>
}

public interface Collection<out E> : Iterable<E> {
    public val size: Int
    public fun isEmpty(): Boolean
    public operator fun contains(element: @UnsafeVariance E): Boolean
    public fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    // iterator() is inherited from Iterable, but we might need to match signature if different?
    // standard Collections.kt defines overrides.
    public override operator fun iterator(): Iterator<E>
}

public interface MutableCollection<E> : Collection<E>, MutableIterable<E> {
    public override operator fun iterator(): MutableIterator<E>
    public fun add(element: E): Boolean
    public fun remove(element: E): Boolean
    public fun addAll(elements: Collection<E>): Boolean
    public fun removeAll(elements: Collection<E>): Boolean
    public fun retainAll(elements: Collection<E>): Boolean
    public fun clear(): Unit
}

public interface List<out E> : Collection<E> {
    public operator fun get(index: Int): E
    public fun indexOf(element: @UnsafeVariance E): Int
    public fun lastIndexOf(element: @UnsafeVariance E): Int
    public fun listIterator(): ListIterator<E>
    public fun listIterator(index: Int): ListIterator<E>
    public fun subList(fromIndex: Int, toIndex: Int): List<E>
}

public interface MutableList<E> : List<E>, MutableCollection<E> {
    public fun add(index: Int, element: E): Unit
    public fun removeAt(index: Int): E
    public fun set(index: Int, element: E): E
    public fun addAll(index: Int, elements: Collection<E>): Boolean
    public override fun listIterator(): MutableListIterator<E>
    public override fun listIterator(index: Int): MutableListIterator<E>
    public override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}

public interface Set<out E> : Collection<E>
public interface MutableSet<E> : Set<E>, MutableCollection<E>

public interface Map<K, out V> {
    public val size: Int
    public fun isEmpty(): Boolean
    public fun containsKey(key: K): Boolean
    public fun containsValue(value: @UnsafeVariance V): Boolean
    public operator fun get(key: K): V?
    public val keys: Set<K>
    public val values: Collection<V>
    public val entries: Set<Entry<K, V>>

    public interface Entry<out K, out V> {
        public val key: K
        public val value: V
    }
}

public interface MutableMap<K, V> : Map<K, V> {
    public fun put(key: K, value: V): V?
    public fun remove(key: K): V?
    public fun putAll(from: Map<out K, V>): Unit
    public fun clear(): Unit
    public override val keys: MutableSet<K>
    public override val values: MutableCollection<V>
    public override val entries: MutableSet<MutableEntry<K, V>>

    public interface MutableEntry<K, V> : Map.Entry<K, V> {
        public fun setValue(newValue: V): V
    }
}

private object EmptyList : List<Nothing> {
    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(element: Nothing): Boolean = false
    override fun containsAll(elements: Collection<Nothing>): Boolean = elements.isEmpty()
    override fun get(index: Int): Nothing = throw IndexOutOfBoundsException()
    override fun indexOf(element: Nothing): Int = -1
    override fun lastIndexOf(element: Nothing): Int = -1
    override fun iterator(): Iterator<Nothing> = EmptyIterator
    override fun listIterator(): ListIterator<Nothing> = EmptyIterator
    override fun listIterator(index: Int): ListIterator<Nothing> = EmptyIterator
    override fun subList(fromIndex: Int, toIndex: Int): List<Nothing> = this
}

private object EmptyIterator : ListIterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun hasPrevious(): Boolean = false
    override fun nextIndex(): Int = 0
    override fun previousIndex(): Int = -1
    override fun next(): Nothing = throw IndexOutOfBoundsException()
    override fun previous(): Nothing = throw IndexOutOfBoundsException()
}

public fun <T> emptyList(): List<T> = EmptyList
public fun <T> listOf(vararg elements: T): List<T> = EmptyList // Dummy

public fun <T> Iterable<T>.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): String = "stub"
