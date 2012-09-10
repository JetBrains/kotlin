package java.util

import java.lang.*

library("collectionsMax")
public fun max<T>(col : Collection<T>, comp : Comparator<T>) : T = js.noImpl

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

    override fun addAll(c: Collection<out E>): Boolean = js.noImpl
    override fun containsAll(c : Collection<out Any?>) : Boolean = js.noImpl
    override fun removeAll(c : Collection<out Any?>) : Boolean = js.noImpl
    override fun retainAll(c : Collection<out Any?>) : Boolean = js.noImpl

    override fun clear(): Unit = js.noImpl
    override fun size(): Int = js.noImpl

    override fun hashCode() : Int = js.noImpl
    override fun equals(other : Any?) : Boolean = js.noImpl
}

library
public abstract class AbstractList<E>(): AbstractCollection<E>(), MutableList<E> {
    override fun get(index: Int): E = js.noImpl
    override fun set(index: Int, element: E): E = js.noImpl

    library("addAt")
    override fun add(index: Int, element: E): Unit = js.noImpl
    override fun addAll(index : Int, c : Collection<out E>) : Boolean = js.noImpl

    library("removeAt")
    override fun remove(index: Int): E = js.noImpl

    override fun indexOf(o: Any?): Int = js.noImpl
    override fun lastIndexOf(o : Any?) : Int = js.noImpl

    override fun listIterator() : MutableListIterator<E> = js.noImpl
    override fun listIterator(index : Int) : MutableListIterator<E> = js.noImpl

    override fun subList(fromIndex : Int, toIndex : Int) : MutableList<E> = js.noImpl
}

library
public open class ArrayList<E>() : AbstractList<E>() {
}

library
public open class HashSet<E>(): AbstractCollection<E>(), MutableSet<E> {
}

library
public open class HashMap<K, V>() : MutableMap<K, V> {
    public override fun size() : Int = js.noImpl
    public override fun isEmpty() : Boolean = js.noImpl
    public override fun get(key : Any?) : V? = js.noImpl
    public override fun containsKey(key : Any?) : Boolean = js.noImpl
    public override fun put(key : K, value : V) : V = js.noImpl
    public override fun putAll(m : Map<out K, out V>) : Unit = js.noImpl
    public override fun remove(key : Any?) : V? = js.noImpl
    public override fun clear() : Unit = js.noImpl
    public override fun containsValue(value : Any?) : Boolean = js.noImpl
    public override fun keySet() : MutableSet<K> = js.noImpl
    public override fun values() : MutableCollection<V> = js.noImpl
    public override fun entrySet() : MutableSet<MutableMap.MutableEntry<K, V>> = js.noImpl
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