package java.util
import java.io.*
import java.util.Map.Entry
public open class HashMap<erased K, erased V>(m : java.util.Map<out K, out V>) : java.util.AbstractMap<K, V>(),
                                                                                 java.util.Map<K, V>,
                                                                                 java.lang.Cloneable,
                                                                                 java.io.Serializable {
    public this() {}
    public this(initialCapacity : Int) {}
    public this(initialCapacity : Int, loadFactor : Float) {}
    override public fun size() : Int {}
    override public fun isEmpty() : Boolean {}
    override public fun get(key : Any?) : V? {}
    override public fun containsKey(key : Any?) : Boolean {}
    override public fun put(key : K, value : V) : V? {}
    override public fun putAll(m : java.util.Map<out K, out V>) : Unit {}
    override public fun remove(key : Any?) : V? {}
    override public fun clear() : Unit {}
    override public fun containsValue(value : Any?) : Boolean {}
    override public fun clone() : Any? {}
    override public fun keySet() : java.util.Set<K> {}
    override public fun values() : java.util.Collection<V> {}
    override public fun entrySet() : java.util.Set<Entry<K, V>> {}
}
