package java.util

import java.lang.*

library("collectionsMax")
public fun max<T>(col : jet.Collection<T>, comp : Comparator<T>) : T = js.noImpl

library
public trait Comparator<T> {
    fun compare(obj1 : T, obj2 : T) : Int;
}

library
public abstract class AbstractCollection<E>() : MutableCollection<E> {
    override fun toArray(): Array<Any?> = js.noImpl
    override fun <T> toArray(a : Array<out T>) : Array<T> = js.noImpl

    override fun isEmpty(): Boolean = js.noImpl
    override fun contains(o: Any?): Boolean = js.noImpl
    override fun iterator(): MutableIterator<E> = js.noImpl

    override fun add(e: E): Boolean = js.noImpl
    override fun remove(o: Any?): Boolean = js.noImpl

    override fun addAll(c: jet.Collection<E>): Boolean = js.noImpl
    override fun containsAll(c : jet.Collection<Any?>) : Boolean = js.noImpl
    override fun removeAll(c : jet.Collection<Any?>) : Boolean = js.noImpl
    override fun retainAll(c : jet.Collection<Any?>) : Boolean = js.noImpl

    override fun clear(): Unit = js.noImpl
    override fun size(): Int = js.noImpl

    override fun hashCode() : Int = js.noImpl
    override fun equals(other : Any?) : Boolean = js.noImpl
}

public native abstract class AbstractList<E>(): AbstractCollection<E>(), MutableList<E> {
    override fun get(index: Int): E = js.noImpl
    override fun set(index: Int, element: E): E = js.noImpl

    override fun add(e: E): Boolean = js.noImpl
    override fun add(index: Int, element: E): Unit = js.noImpl
    override fun addAll(index: Int, c: jet.Collection<E>) : Boolean = js.noImpl

    override fun remove(index: Int): E = js.noImpl

    override fun indexOf(o: Any?): Int = js.noImpl
    override fun lastIndexOf(o: Any?): Int = js.noImpl

    override fun listIterator() : MutableListIterator<E> = js.noImpl
    override fun listIterator(index : Int) : MutableListIterator<E> = js.noImpl

    override fun subList(fromIndex : Int, toIndex : Int) : MutableList<E> = js.noImpl

    override fun equals(other: Any?): Boolean = js.noImpl

    fun toString(): String = js.noImpl
    override fun size(): Int = js.noImpl
}

public native class ArrayList<E>(): AbstractList<E>() {
}

// JS array is sparse, so, there is no any difference between ArrayList and LinkedList
public native class LinkedList<E>(): AbstractList<E>() {
    public fun poll(): E? = js.noImpl
    public fun peek(): E? = js.noImpl
    public fun offer(e: E): Boolean = js.noImpl
}

public library class HashSet<E>(): AbstractCollection<E>(), MutableSet<E> {
}

library
public open class HashMap<K, V>(): MutableMap<K, V> {
    override public fun size(): Int = js.noImpl
    override public fun isEmpty(): Boolean = js.noImpl
    override public fun get(key: Any?): V? = js.noImpl
    override public  fun containsKey(key: Any?): Boolean = js.noImpl
    override public fun put(key: K, value: V): V = js.noImpl
    override public fun putAll(m: jet.Map<out K, out V>): Unit = js.noImpl
    override public fun remove(key: Any?): V? = js.noImpl
    override public fun clear(): Unit = js.noImpl
    override public fun containsValue(value: Any?): Boolean = js.noImpl
    override public fun keySet(): MutableSet<K> = js.noImpl
    override public fun values(): MutableCollection<V> = js.noImpl
    override public fun entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> = js.noImpl
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
public class NoSuchElementException() : Exception() {}

library
public trait Enumeration<E> {
    open public fun hasMoreElements() : Boolean
    open public fun nextElement() : E
}

native
public class Date() {
    public fun getTime() : Int = js.noImpl
}