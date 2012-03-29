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
    public override fun size() : Int {}
    public override fun isEmpty() : Boolean {}
    public override fun get(key : Any?) : V? {}
    public override fun containsKey(key : Any?) : Boolean {}
    public override fun put(key : K, value : V) : V? {}
    public override fun putAll(m : java.util.Map<out K, out V>) : Unit {}
    public override fun remove(key : Any?) : V? {}
    public override fun clear() : Unit {}
    public override fun containsValue(value : Any?) : Boolean {}
    public override fun clone() : Any? {}
    public override fun keySet() : java.util.Set<K> {}
    public override fun values() : java.util.Collection<V> {}
    public override fun entrySet() : java.util.Set<Entry<K, V>> {}
}
