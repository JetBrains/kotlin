package java.util

public open class ArrayList<E>(initialCapacity : Int = 0) : List<E> {
    override public fun size() : Int
    override public fun isEmpty() : Boolean
    override public fun contains(o : Any?) : Boolean
    override public fun containsAll(c : Collection<*>?) : Boolean
    override public fun indexOf(o : Any?) : Int
    override public fun lastIndexOf(o : Any?) : Int
    override public fun toArray() : Array<Any>
    override public fun toArray<T>(a : Array<T>) : Array<T>
    override public fun get(index : Int) : E
    override public fun set(index : Int, element : E) : E
    override public fun add(e : E) : Boolean
    override public fun add(index : Int, element : E) : Unit
    override public fun remove(index : Int) : E?
    override public fun clear() : Unit
    override public fun addAll(c : Collection<out E?>?) : Boolean
    override public fun addAll(index : Int, c : Collection<out E?>?) : Boolean
    override public fun removeAll(c : Collection<*>?) : Boolean
    override public fun retainAll(c : Collection<*>?) : Boolean
    override public fun iterator() : Iterator<E>
    override public fun subList(fromIndex : Int, toIndex : Int) : List<E?>?
    override public fun remove(o : Any?) : Boolean
}

public trait Collection<E> {
    open public fun size() : Int
    open public fun isEmpty() : Boolean
    open public fun contains(o : Any?) : Boolean
    open public fun iterator() : Iterator<E>
    open public fun toArray() : Array<Any>
    open public fun toArray<T>(a : Array<T>) : Array<T>
    open public fun add(e : E) : Boolean
    open public fun containsAll(c : Collection<*>?) : Boolean
    open public fun addAll(c : Collection<out E?>?) : Boolean
    open public fun removeAll(c : Collection<*>?) : Boolean
    open public fun retainAll(c : Collection<*>?) : Boolean
    open public fun clear() : Unit
    open public fun remove(o : Any?) : Boolean
}


public trait List<E> : Collection<E> {
    override public fun size() : Int
    override public fun isEmpty() : Boolean
    override public fun contains(o : Any?) : Boolean
    override public fun iterator() : Iterator<E>
    override public fun toArray() : Array<Any>
    override public fun toArray<T>(a : Array<T>) : Array<T>
    override public fun add(e : E) : Boolean
    override public fun containsAll(c : Collection<*>?) : Boolean
    override public fun addAll(c : Collection<out E?>?) : Boolean
    open public fun addAll(index : Int, c : Collection<out E?>?) : Boolean
    override public fun removeAll(c : Collection<*>?) : Boolean
    override public fun retainAll(c : Collection<*>?) : Boolean
    override public fun clear() : Unit
    open public fun get(index : Int) : E
    open public fun set(index : Int, element : E) : E
    open public fun add(index : Int, element : E) : Unit
    js_name("remove_foo")
    open public fun remove(index : Int) : E?
    open public fun indexOf(o : Any?) : Int
    open public fun lastIndexOf(o : Any?) : Int
    open public fun subList(fromIndex : Int, toIndex : Int) : List<E?>?
    js_name("remove_foo")
    override public fun remove(o : Any?) : Boolean
}

annotation class js_name(s : String)

trait Set<E> : Collection<E>

public trait Map<K, V> {
    open public fun size() : Int
    open public fun isEmpty() : Boolean
    open public fun containsKey(key : Any?) : Boolean
    open public fun containsValue(value : Any?) : Boolean
    open public fun get(key : Any?) : V
    open public fun put(key : K?, value : V?) : V?
    open public fun remove(key : Any?) : V?
    open public fun putAll(m : Map<out K?, out V?>?) : Unit
    open public fun clear() : Unit
    open public fun keySet() : Set<K>
    open public fun values() : Collection<V?>?
}

public open class HashMap<K, V>() : Map<K, V>{
    override public fun size() : Int
    override public fun isEmpty() : Boolean
    override public fun get(key : Any?) : V
    override public fun containsKey(key : Any?) : Boolean
    override public fun put(key : K?, value : V?) : V?
    override public fun putAll(m : Map<out K?, out V?>?) : Unit
    override public fun remove(key : Any?) : V?
    override public fun clear() : Unit
    override public fun containsValue(value : Any?) : Boolean
    override public fun keySet() : Set<K>
    override public fun values() : Collection<V?>?
}

public class StringBuilder() {
    public fun append(obj : Any) : StringBuilder
    public fun toString() : String
}

