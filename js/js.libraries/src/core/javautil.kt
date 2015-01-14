package java.util

native
private val DEFAULT_INITIAL_CAPACITY = 16

native
private val DEFAULT_LOAD_FACTOR = 0.75f

library
public trait Comparator<T> {
    public fun compare(obj1: T, obj2: T): Int;
}

library
public abstract class AbstractCollection<E>() : MutableCollection<E> {
    override fun isEmpty(): Boolean = noImpl
    override fun contains(o: Any?): Boolean = noImpl
    override fun iterator(): MutableIterator<E> = noImpl

    override fun add(e: E): Boolean = noImpl
    override fun remove(o: Any?): Boolean = noImpl

    override fun addAll(c: Collection<E>): Boolean = noImpl
    override fun containsAll(c: Collection<Any?>): Boolean = noImpl
    override fun removeAll(c: Collection<Any?>): Boolean = noImpl
    override fun retainAll(c: Collection<Any?>): Boolean = noImpl

    override fun clear(): Unit = noImpl
    abstract override fun size(): Int

    override fun hashCode(): Int = noImpl
    override fun equals(other: Any?): Boolean = noImpl
}

library
public abstract class AbstractList<E>() : AbstractCollection<E>(), MutableList<E> {
    abstract override fun get(index: Int): E
    override fun set(index: Int, element: E): E = noImpl

    override fun add(e: E): Boolean = noImpl
    override fun add(index: Int, element: E): Unit = noImpl
    override fun addAll(index: Int, c: Collection<E>): Boolean = noImpl

    override fun remove(index: Int): E = noImpl

    override fun indexOf(o: Any?): Int = noImpl
    override fun lastIndexOf(o: Any?): Int = noImpl

    override fun listIterator(): MutableListIterator<E> = noImpl
    override fun listIterator(index: Int): MutableListIterator<E> = noImpl

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = noImpl

    override fun size(): Int = noImpl

    override fun equals(other: Any?): Boolean = noImpl

    override fun toString(): String = noImpl
}

library
public open class ArrayList<E>(capacity: Int = 0) : AbstractList<E>() {
    override fun get(index: Int): E = noImpl
    override fun size(): Int = noImpl
}

library
public open class LinkedList<E>() : AbstractList<E>() {
    override fun get(index: Int): E = noImpl
    override fun set(index: Int, element: E): E = noImpl
    override fun add(index: Int, element: E): Unit = noImpl

    [suppress("BASE_WITH_NULLABLE_UPPER_BOUND")]
    public fun poll(): E? = noImpl
    [suppress("BASE_WITH_NULLABLE_UPPER_BOUND")]
    public fun peek(): E? = noImpl
    public fun offer(e: E): Boolean = noImpl
}

library
public open class HashSet<E>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR
) : AbstractCollection<E>(), MutableSet<E> {
    override fun size(): Int = noImpl
}

library
public trait SortedSet<E> : Set<E> {
}

library
public open class TreeSet<E>() : AbstractCollection<E>(), MutableSet<E>, SortedSet<E> {
    override fun size(): Int = noImpl
}

library
public open class LinkedHashSet<E>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR
) : HashSet<E>(initialCapacity, loadFactor), MutableSet<E> {
    override fun size(): Int = noImpl
}

library
public open class HashMap<K, V>(initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR) : MutableMap<K, V> {
    override fun size(): Int = noImpl
    override fun isEmpty(): Boolean = noImpl
    [suppress("BASE_WITH_NULLABLE_UPPER_BOUND")]
    override fun get(key: Any?): V? = noImpl
    override fun containsKey(key: Any?): Boolean = noImpl
    [suppress("BASE_WITH_NULLABLE_UPPER_BOUND")]
    override fun put(key: K, value: V): V? = noImpl
    override fun putAll(m: Map<out K, V>): Unit = noImpl
    [suppress("BASE_WITH_NULLABLE_UPPER_BOUND")]
    override fun remove(key: Any?): V? = noImpl
    override fun clear(): Unit = noImpl
    override fun containsValue(value: Any?): Boolean = noImpl
    override fun keySet(): MutableSet<K> = noImpl
    override fun values(): MutableCollection<V> = noImpl
    override fun entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = noImpl
}

library
public open class LinkedHashMap<K, V>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR, accessOrder: Boolean = false
) : HashMap<K, V>(initialCapacity, loadFactor)

library
public class NoSuchElementException(message: String? = null) : Exception() {}

library
public trait Enumeration<E> {
    public fun hasMoreElements(): Boolean
    public fun nextElement(): E
}

native
public class Date() {
    public fun getTime(): Int = noImpl
}
