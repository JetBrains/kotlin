package java.util

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