package java.util

import java.util.Map.Entry

public abstract class AbstractMap<erased K, erased V> protected () : java.lang.Object(), java.util.Map<K, V> {
    public override fun size() : Int {}
    public override fun isEmpty() : Boolean {}
    public override fun containsValue(value : Any?) : Boolean {}
    public override fun containsKey(key : Any?) : Boolean {}
    public override fun get(key : Any?) : V? {}
    public override fun put(key : K, value : V) : V? {}
    public override fun remove(key : Any?) : V? {}
    public override fun putAll(m : java.util.Map<out K, out V>) : Unit {}
    public override fun clear() : Unit {}
    public override fun keySet() : java.util.Set<K> {}
    public override fun values() : java.util.Collection<V> {}
    public abstract override fun entrySet() : java.util.Set<Entry<K, V>>
    //public override fun equals(o : Any?) : Boolean
    //public override fun hashCode() : Int
    //public override fun toString() : java.lang.String?
}
