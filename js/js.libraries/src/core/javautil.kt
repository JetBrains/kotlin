package java.util

native
private val DEFAULT_INITIAL_CAPACITY = 16

native
private val DEFAULT_LOAD_FACTOR = 0.75f

library
public trait Comparator<T> {
    fun compare(obj1: T, obj2: T): Int;
}

library
public abstract class AbstractCollection<E>() : MutableCollection<E> {
    override fun isEmpty(): Boolean = js.noImpl
    override fun contains(o: Any?): Boolean = js.noImpl
    override fun iterator(): MutableIterator<E> = js.noImpl

    override fun add(e: E): Boolean = js.noImpl
    override fun remove(o: Any?): Boolean = js.noImpl

    override fun addAll(c: Collection<E>): Boolean = js.noImpl
    override fun containsAll(c: Collection<Any?>): Boolean = js.noImpl
    override fun removeAll(c: Collection<Any?>): Boolean = js.noImpl
    override fun retainAll(c: Collection<Any?>): Boolean = js.noImpl

    override fun clear(): Unit = js.noImpl
    override fun size(): Int = js.noImpl

    override fun hashCode(): Int = js.noImpl
    override fun equals(other: Any?): Boolean = js.noImpl
}

library
public abstract class AbstractList<E>() : AbstractCollection<E>(), MutableList<E> {
    override fun get(index: Int): E = js.noImpl
    override fun set(index: Int, element: E): E = js.noImpl

    override fun add(e: E): Boolean = js.noImpl
    override fun add(index: Int, element: E): Unit = js.noImpl
    override fun addAll(index: Int, c: Collection<E>): Boolean = js.noImpl

    override fun remove(index: Int): E = js.noImpl

    override fun indexOf(o: Any?): Int = js.noImpl
    override fun lastIndexOf(o: Any?): Int = js.noImpl

    override fun listIterator(): MutableListIterator<E> = js.noImpl
    override fun listIterator(index: Int): MutableListIterator<E> = js.noImpl

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = js.noImpl

    override fun size(): Int = js.noImpl

    override fun equals(other: Any?): Boolean = js.noImpl

    override fun toString(): String = js.noImpl
}

library
public open class ArrayList<E>(capacity: Int = 0) : AbstractList<E>() {
}

library
public open class LinkedList<E>() : AbstractList<E>() {
    public override fun get(index: Int): E = js.noImpl
    public override fun set(index: Int, element: E): E = js.noImpl
    public override fun add(index: Int, element: E): Unit = js.noImpl
    public fun poll(): E? = js.noImpl
    public fun peek(): E? = js.noImpl
    public fun offer(e: E): Boolean = js.noImpl
}

library
public open class HashSet<E>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR
) : AbstractCollection<E>(), MutableSet<E>

library
public trait SortedSet<E> : Set<E> {
}

library
public open class TreeSet<E>() : AbstractCollection<E>(), MutableSet<E>, SortedSet<E> {
}

library
public open class LinkedHashSet<E>(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR
) : HashSet<E>(initialCapacity, loadFactor), MutableSet<E>

library
public open class HashMap<K, V>(initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, loadFactor: Float = DEFAULT_LOAD_FACTOR) : MutableMap<K, V> {
    public override fun size(): Int = js.noImpl
    public override fun isEmpty(): Boolean = js.noImpl
    public override fun get(key: Any?): V? = js.noImpl
    public override fun containsKey(key: Any?): Boolean = js.noImpl
    public override fun put(key: K, value: V): V = js.noImpl
    public override fun putAll(m: Map<out K, out V>): Unit = js.noImpl
    public override fun remove(key: Any?): V? = js.noImpl
    public override fun clear(): Unit = js.noImpl
    public override fun containsValue(value: Any?): Boolean = js.noImpl
    public override fun keySet(): MutableSet<K> = js.noImpl
    public override fun values(): MutableCollection<V> = js.noImpl
    public override fun entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = js.noImpl
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
    public fun getTime(): Int = js.noImpl
}
