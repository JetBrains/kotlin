package java.util

import java.lang.*

library("collectionsMax")
public fun max<T>(col : Collection<T>, comp : Comparator<T>) : T = js.noImpl

library
public trait Comparator<T> {
    fun compare(obj1 : T, obj2 : T) : Int;
}

library
public trait Iterator<T> {
    open public fun next() : T = js.noImpl
    open public fun hasNext() : Boolean = js.noImpl
    open public fun remove() : Unit = js.noImpl
}

public object Collections {
    library("collectionsMax")
    public fun max<T>(col : Collection<T>, comp : Comparator<T>) : T = js.noImpl

    // TODO should be immutable!
    public val emptyList: List<Any> = ArrayList<Any>()
    public val emptyMap: Map<Any, Any> = HashMap<Any,Any>()

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
    open public fun size(): Int
    open public fun isEmpty(): Boolean
    open public fun contains(o: Any?): Boolean
    override public fun iterator(): Iterator<E>
    public fun toArray(): Array<E>
    // open public fun toArray<T>(a : Array<out T>) : Array<T>
    open public fun add(e: E): Boolean
    open public fun remove(o: Any?): Boolean
    //open public fun containsAll(c : java.util.Collection<*>) : Boolean
    open public fun addAll(c: Collection<out E>): Boolean
    //open public fun removeAll(c : java.util.Collection<*>) : Boolean
    //open public fun retainAll(c : java.util.Collection<*>) : Boolean
    open public fun clear(): Unit
}

library
public abstract class AbstractCollection<E>() : Collection<E> {
    override public fun toArray(): Array<E> = js.noImpl

    override public fun isEmpty(): Boolean = js.noImpl
    override public fun contains(o: Any?): Boolean = js.noImpl
    override public fun iterator(): Iterator<E> = js.noImpl

    override public fun add(e: E): Boolean = js.noImpl
    override public fun remove(o: Any?): Boolean = js.noImpl

    override public fun addAll(c: Collection<out E>): Boolean = js.noImpl

    override public fun clear(): Unit = js.noImpl
    override public fun size(): Int = js.noImpl
}

library
public trait List<E>: Collection<E> {
    public fun get(index: Int): E
    public fun set(index: Int, element: E): E

    public fun add(index: Int, element: E): Unit
    public fun remove(index: Int): E

    public fun indexOf(o: E?): Int
}

library
public abstract class AbstractList<E>(): AbstractCollection<E>(), List<E> {
    override public  fun get(index: Int): E = js.noImpl
    override public  fun set(index: Int, element: E): E = js.noImpl

    library("addAt")
    override public  public  fun add(index: Int, element: E): Unit = js.noImpl

    library("removeAt")
    override public  fun remove(index: Int): E = js.noImpl

    override public  fun indexOf(o: E?): Int = js.noImpl
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
    open public fun size() : Int
    open public fun isEmpty() : Boolean
    open public fun containsKey(key : Any?) : Boolean
    open public fun containsValue(value : Any?) : Boolean
    open public fun get(key : Any?) : V?
    open public fun put(key : K, value : V) : V?
    open public fun remove(key : Any?) : V?
    open public fun putAll(m : java.util.Map<out K, out V>) : Unit
    open public fun clear() : Unit
    open public fun keySet() : java.util.Set<K>
    open public fun values() : java.util.Collection<V>

    open public fun entrySet() : java.util.Set<Entry<K, V>>
// open public fun equals(o : Any?) : Boolean
// open public fun hashCode() : Int

    public trait Entry<K, V> {
        open public fun getKey() : K
        open public fun getValue() : V
        open public fun setValue(value : V) : V
// open public fun equals(o : Any?) : Boolean
// open public fun hashCode() : Int
    }
}

library
public open class HashMap<K, V>() : Map<K, V> {
    public override fun size() : Int = js.noImpl
    public override fun isEmpty() : Boolean = js.noImpl
    public override fun get(key : Any?) : V? = js.noImpl
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
    override public fun append(c: Char): Appendable? = js.noImpl
    override public fun append(csq: CharSequence?): Appendable? = js.noImpl
    override public fun append(csq: CharSequence?, start: Int, end: Int): Appendable? = js.noImpl
    public fun append(obj : Any?) : StringBuilder = js.noImpl
    public fun toString() : String = js.noImpl
}

library
public class NoSuchElementException() : Exception() {}

library
public trait Enumeration<E> {
    open public fun hasMoreElements() : Boolean
    open public fun nextElement() : E
}