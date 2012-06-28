package java.util

import java.lang.*

library("collectionsMax")
public fun max<T>(col : Collection<T>, comp : Comparator<T>) : T = js.noImpl

library
public trait Comparator<T> {
    fun compare(obj1 : T, obj2 : T) : Int;
}

library("comparator")
public fun comparator<T>(f : (T, T) -> Int) : Comparator<T> = js.noImpl


library
public trait Iterator<T> {
    open fun next() : T = js.noImpl
    open fun hasNext() : Boolean = js.noImpl
    open fun remove() : Unit = js.noImpl
}

val Collections = object {
    library("collectionsMax")
    public fun max<T>(col : Collection<T>, comp : Comparator<T>) : T = js.noImpl

    // TODO should be immutable!
    private val emptyList = ArrayList<Any>()
    private val emptyMap = HashMap<Any,Any>()

    public val <T> EMPTY_LIST: List<T>
    get() = emptyList<T>()

    public val <K,V> EMPTY_MAP: Map<K,V>
    get() = emptyMap<K,V>()

    public fun <T> emptyList(): List<T> = emptyList as List<T>
    public fun <K,V> emptyMap(): Map<K,V> = emptyMap as Map<K,V>

    public fun <in T> sort(list: List<T>): Unit {
        throw UnsupportedOperationException()
    }

    public fun <in T> sort(list: List<T>, comparator: java.util.Comparator<T>): Unit {
        throw UnsupportedOperationException()
    }

    public fun <T> reverse(list: List<T>): Unit {
        val size = list.size()
        for (i in 0.upto(size / 2)) {
            val i2 = size - i
            val tmp = list[i]
            list[i] = list[i2]
            list[i2] = tmp
        }
    }
}

library
public trait Collection<E>: Iterable<E> {
    open fun size(): Int
    open fun isEmpty(): Boolean
    open fun contains(o: Any?): Boolean
    override fun iterator(): Iterator<E>
    fun toArray(): Array<E>
    // open fun toArray<T>(a : Array<out T>) : Array<T>
    open fun add(e: E): Boolean
    open fun remove(o: Any?): Boolean
    //open fun containsAll(c : java.util.Collection<*>) : Boolean
    open fun addAll(c: Collection<out E>): Boolean
    //open fun removeAll(c : java.util.Collection<*>) : Boolean
    //open fun retainAll(c : java.util.Collection<*>) : Boolean
    open fun clear(): Unit
}

library
public abstract class AbstractCollection<E>() : Collection<E> {
    override fun toArray(): Array<E> = js.noImpl

    override fun isEmpty(): Boolean = js.noImpl
    override fun contains(o: Any?): Boolean = js.noImpl
    override fun iterator(): Iterator<E> = js.noImpl

    override fun add(e: E): Boolean = js.noImpl
    override fun remove(o: Any?): Boolean = js.noImpl

    override fun addAll(c: Collection<out E>): Boolean = js.noImpl

    override fun clear(): Unit = js.noImpl
    override fun size(): Int = js.noImpl
}

library
public trait List<E>: Collection<E> {
    fun get(index: Int): E
    fun set(index: Int, element: E): E

    fun add(index: Int, element: E): Unit
    fun remove(index: Int): E

    fun indexOf(o: E?): Int
}

library
public abstract class AbstractList<E>(): AbstractCollection<E>(), List<E> {
    override fun get(index: Int): E = js.noImpl
    override fun set(index: Int, element: E): E = js.noImpl

    library("addAt")
    override fun add(index: Int, element: E): Unit = js.noImpl

    library("removeAt")
    override fun remove(index: Int): E = js.noImpl

    override fun indexOf(o: E?): Int = js.noImpl
}

library
public open class ArrayList<E>() : AbstractList<E>() {
}

library
public trait Set<E> : Collection<E> {
}

library
public open class HashSet<E>(): AbstractCollection<E>(), java.util.Set<E> {
}

library
public trait Map<K, V> {
    open fun size() : Int
    open fun isEmpty() : Boolean
    open fun containsKey(key : Any?) : Boolean
    open fun containsValue(value : Any?) : Boolean
    open fun get(key : Any?) : V?
    open fun put(key : K, value : V) : V?
    open fun remove(key : Any?) : V?
    open fun putAll(m : java.util.Map<out K, out V>) : Unit
    open fun clear() : Unit
    open fun keySet() : java.util.Set<K>
    open fun values() : java.util.Collection<V>

    open fun entrySet() : java.util.Set<Entry<K, V>>
//    open fun equals(o : Any?) : Boolean
//    open fun hashCode() : Int

    trait Entry<K, V> {
        open fun getKey() : K
        open fun getValue() : V
        open fun setValue(value : V) : V
//            open fun equals(o : Any?) : Boolean
//            open fun hashCode() : Int
    }
}

library
public open class HashMap<K, V>() : java.util.Map<K, V> {
    public override fun size() : Int = js.noImpl
    public override fun isEmpty() : Boolean = js.noImpl
    public override fun get(key : Any?) : V = js.noImpl
    public override fun containsKey(key : Any?) : Boolean = js.noImpl
    public override fun put(key : K, value : V) : V = js.noImpl
    public override fun putAll(m : java.util.Map<out K, out V>) : Unit = js.noImpl
    public override fun remove(key : Any?) : V? = js.noImpl
    public override fun clear() : Unit = js.noImpl
    public override fun containsValue(value : Any?) : Boolean = js.noImpl
    public override fun keySet() : java.util.Set<K> = js.noImpl
    public override fun values() : java.util.Collection<V> = js.noImpl
    public override fun entrySet() : java.util.Set<Map.Entry<K, V>> = js.noImpl
}

library
public open class LinkedList<E>(): AbstractList<E>() {
    public override fun get(index: Int): E = js.noImpl
    public override fun set(index: Int, element: E): E = js.noImpl
    public override fun add(index: Int, element: E): Unit = js.noImpl
    public fun poll(): E? = js.noImpl
    public fun peek(): E? = js.noImpl
    public fun offer(e: E): Boolean = js.noImpl
}

library
public class StringBuilder() : Appendable {
    override fun append(c: Char): Appendable? = js.noImpl
    override fun append(csq: CharSequence?): Appendable? = js.noImpl
    override fun append(csq: CharSequence?, start: Int, end: Int): Appendable? = js.noImpl
    public fun append(obj : Any?) : StringBuilder = js.noImpl
    public fun toString() : String = js.noImpl
}

library
class NoSuchElementException() : Exception() {}
