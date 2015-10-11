package java.util

@native
private val DEFAULT_INITIAL_CAPACITY = 16

@native
private val DEFAULT_LOAD_FACTOR = 0.75f

@library
public interface Comparator<T> {
    public fun compare(obj1: T, obj2: T): Int;
}

@library
public abstract class AbstractCollection<E>() : MutableCollection<E> {
    override val isEmpty: Boolean get() = noImpl
    override fun contains(o: E): Boolean = noImpl
    override fun iterator(): MutableIterator<E> = noImpl

    override fun add(e: E): Boolean = noImpl
    override fun remove(o: E): Boolean = noImpl

    override fun addAll(c: Collection<E>): Boolean = noImpl
    override fun containsAll(c: Collection<E>): Boolean = noImpl
    override fun removeAll(c: Collection<Any?>): Boolean = noImpl
    override fun retainAll(c: Collection<Any?>): Boolean = noImpl

    override fun clear(): Unit = noImpl
    abstract override val size: Int

    override fun hashCode(): Int = noImpl
    override fun equals(other: Any?): Boolean = noImpl
}

@library
public abstract class AbstractList<E>() : AbstractCollection<E>(), MutableList<E> {
    abstract override fun get(index: Int): E
    override fun set(index: Int, element: E): E = noImpl

    override fun add(e: E): Boolean = noImpl
    override fun add(index: Int, element: E): Unit = noImpl
    override fun addAll(index: Int, c: Collection<E>): Boolean = noImpl

    override fun removeAt(index: Int): E = noImpl

    override fun indexOf(o: Any?): Int = noImpl
    override fun lastIndexOf(o: Any?): Int = noImpl

    override fun listIterator(): MutableListIterator<E> = noImpl
    override fun listIterator(index: Int): MutableListIterator<E> = noImpl

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = noImpl

    override val size: Int get() = noImpl

    override fun equals(other: Any?): Boolean = noImpl

    override fun toString(): String = noImpl
}

@library
public open class ArrayList<E>(capacity: Int = 0) : AbstractList<E>() {
    override fun get(index: Int): E = noImpl
    override val size: Int get() = noImpl
}

@library
public open class LinkedList<E>() : AbstractList<E>() {
    override fun get(index: Int): E = noImpl
    override fun set(index: Int, element: E): E = noImpl
    override fun add(index: Int, element: E): Unit = noImpl

    @Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
    public fun poll(): E? = noImpl
    @Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
    public fun peek(): E? = noImpl
    public fun offer(e: E): Boolean = noImpl
}

@library
public open class HashSet<E>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR
) : AbstractCollection<E>(), MutableSet<E> {
    override val size: Int get() = noImpl
}

@library
public interface SortedSet<E> : Set<E> {
}

@library
public open class TreeSet<E>() : AbstractCollection<E>(), MutableSet<E>, SortedSet<E> {
    override val size: Int get() = noImpl
}

@library
public open class LinkedHashSet<E>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR
) : HashSet<E>(initialCapacity, loadFactor), MutableSet<E> {
    override val size: Int get() = noImpl
}

@library
public open class HashMap<K, V>(initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR) : MutableMap<K, V> {
    override val size: Int get() = noImpl
    override val isEmpty: Boolean get() = noImpl
    @Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
    override fun get(key: Any?): V? = noImpl
    override fun containsKey(key: Any?): Boolean = noImpl
    @Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
    override fun put(key: K, value: V): V? = noImpl
    override fun putAll(m: Map<out K, V>): Unit = noImpl
    @Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
    override fun remove(key: Any?): V? = noImpl
    override fun clear(): Unit = noImpl
    override fun containsValue(value: Any?): Boolean = noImpl
    override fun keySet(): MutableSet<K> = noImpl
    override fun values(): MutableCollection<V> = noImpl
    override fun entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = noImpl
}

@library
public open class LinkedHashMap<K, V>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR, accessOrder: Boolean = false
) : HashMap<K, V>(initialCapacity, loadFactor)

@library
public open class NoSuchElementException(message: String? = null) : Exception() {}

@library
public interface Enumeration<E> {
    public fun hasMoreElements(): Boolean
    public fun nextElement(): E
}

@native
public class Date() {
    public fun getTime(): Int = noImpl
}
