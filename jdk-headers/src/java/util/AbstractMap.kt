package java.util

import java.util.Map.Entry

abstract public open class AbstractMap<erased K, erased V> protected () : java.lang.Object(), java.util.Map<K, V> {
    override public fun size() : Int {}
    override public fun isEmpty() : Boolean {}
    override public fun containsValue(value : Any?) : Boolean {}
    override public fun containsKey(key : Any?) : Boolean {}
    override public fun get(key : Any?) : V? {}
    override public fun put(key : K, value : V) : V? {}
    override public fun remove(key : Any?) : V? {}
    override public fun putAll(m : java.util.Map<out K, out V>) : Unit {}
    override public fun clear() : Unit {}
    override public fun keySet() : java.util.Set<K> {}
    override public fun values() : java.util.Collection<V> {}
    abstract override public fun entrySet() : java.util.Set<Entry<K, V>>
    //override public fun equals(o : Any?) : Boolean
    //override public fun hashCode() : Int
    //override public fun toString() : java.lang.String?
}
