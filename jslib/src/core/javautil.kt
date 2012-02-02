package java.util

import js.annotations.*;

LibraryClass
public trait Comparator<T> {
    fun compare(obj1 : T, obj2 : T) : Int;
}

LibraryFun("comparator")
public fun comparator<T>(f : (T, T) -> Int) : Comparator<T> {}


LibraryClass
public open class Iterator<T>() {
    open fun next() : T {}
    open fun hasNext() : Boolean {}
}

LibraryClass
public open class ArrayList<erased E>() : java.util.List<E> {
    override public fun size() : Int {}
    override public fun isEmpty() : Boolean {}
    override public fun contains(o : Any?) : Boolean {}
    override public fun iterator() : Iterator<E> {}
   // override public fun indexOf(o : Any?) : Int {}
  //  override public fun lastIndexOf(o : Any?) : Int {}
  //  override public fun toArray() : Array<Any?> {}
  //  override public fun toArray<erased T>(a : Array<out T>) : Array<T> {}
    override public fun get(index : Int) : E {}
    override public fun set(index : Int, element : E) : E {}
    override public fun add(e : E) : Boolean {}
    override public fun add(index : Int, element : E) : Unit {}
    override public fun remove(index : Int) : E {}
    override public fun remove(o : Any?) : Boolean {}
    override public fun clear() : Unit {}
    override public fun addAll(c : java.util.Collection<out E>) : Boolean {}
  //  override public fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean {}
}

LibraryClass
public trait Collection<erased E> : java.lang.Iterable<E> {
    open fun size() : Int
    open fun isEmpty() : Boolean
    open fun contains(o : Any?) : Boolean
    override fun iterator() : java.util.Iterator<E>
   // open fun toArray() : Array<Any?>
   // open fun toArray<erased T>(a : Array<out T>) : Array<T>
    open fun add(e : E) : Boolean
    open fun remove(o : Any?) : Boolean
    //open fun containsAll(c : java.util.Collection<*>) : Boolean
    open fun addAll(c : java.util.Collection<out E>) : Boolean
    //open fun removeAll(c : java.util.Collection<*>) : Boolean
    //open fun retainAll(c : java.util.Collection<*>) : Boolean
    open fun clear() : Unit
}

LibraryClass
public trait List<erased E> : java.util.Collection<E> {
    override fun size() : Int
    override fun isEmpty() : Boolean
    override fun contains(o : Any?) : Boolean
    override fun iterator() : java.util.Iterator<E>
  //  override fun toArray() : Array<Any?>
    // Simulate Java's array covariance
 //   override fun toArray<erased T>(a : Array<out T>) : Array<T>
    override fun add(e : E) : Boolean
    override fun remove(o : Any?) : Boolean
  //  override fun containsAll(c : java.util.Collection<*>) : Boolean
    override fun addAll(c : java.util.Collection<out E>) : Boolean
   // open fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean
   // override fun removeAll(c : java.util.Collection<*>) : Boolean
   // override fun retainAll(c : java.util.Collection<*>) : Boolean
    override fun clear() : Unit
    open fun get(index : Int) : E
    open fun set(index : Int, element : E) : E
    open fun add(index : Int, element : E) : Unit
    open fun remove(index : Int) : E
   // open fun indexOf(o : Any?) : Int
   // open fun lastIndexOf(o : Any?) : Int
}

LibraryClass
public trait Set<erased E> : java.util.Collection<E> {
    override fun size() : Int
    override fun isEmpty() : Boolean
    override fun contains(o : Any?) : Boolean
    override fun iterator() : java.util.Iterator<E>
  //  override fun toArray() : Array<Any?>
  //  override fun toArray<erased T>(a : Array<out T>) : Array<T>
    override fun add(e : E) : Boolean
    override fun remove(o : Any?) : Boolean
    //override fun containsAll(c : java.util.Collection<*>) : Boolean
    override fun addAll(c : java.util.Collection<out E>) : Boolean
    //override fun retainAll(c : java.util.Collection<*>) : Boolean
    //override fun removeAll(c : java.util.Collection<*>) : Boolean
    override fun clear() : Unit
}

LibraryClass
public open class HashSet<erased E>() : java.util.Set<E> {
    override public fun iterator() : java.util.Iterator<E> {}
    override public fun size() : Int {}
    override public fun isEmpty() : Boolean {}
    override public fun contains(o : Any?) : Boolean {}
    override public fun add(e : E) : Boolean {}
    override public fun remove(o : Any?) : Boolean {}
    override public fun clear() : Unit {}
    override fun addAll(c : java.util.Collection<out E>) : Boolean
}

LibraryClass
public trait Map<erased K, erased V> {
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
}

LibraryClass
public open class HashMap<erased K, erased V>() : java.util.Map<K, V> {
    override public fun size() : Int {}
    override public fun isEmpty() : Boolean {}
    override public fun get(key : Any?) : V {}
    override public fun containsKey(key : Any?) : Boolean {}
    override public fun put(key : K, value : V) : V {}
    override public fun putAll(m : java.util.Map<out K, out V>) : Unit {}
    override public fun remove(key : Any?) : V? {}
    override public fun clear() : Unit {}
    override public fun containsValue(value : Any?) : Boolean {}
    override public fun keySet() : java.util.Set<K> {}
    override public fun values() : java.util.Collection<V> {}
}

LibraryClass
public class StringBuilder() {
    public fun append(obj : Any) : StringBuilder
    public fun toString() : String
}

