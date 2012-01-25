package java.util
import java.io.*
public open class LinkedHashMap<erased K, erased V>(m : java.util.Map<out K, out V>) : java.util.HashMap<K, V>(), java.util.Map<K, V>, java.lang.Object {
    public this(initialCapacity : Int, loadFactor : Float) {}
    public this(initialCapacity : Int) {}
    public this() {}

    override public fun containsValue(value : Any?) : Boolean {}
    override public fun get(key : Any?) : V? {}
    override public fun clear() : Unit {}
    open protected fun removeEldestEntry(eldest : java.util.Map.Entry<K, V>) : Boolean {}
    //class object {
    //open public fun init<K, V>(initialCapacity : Int, loadFactor : Float) : LinkedHashMap<K, V> {
    //val __ = LinkedHashMap(0, null, false)
    //return __
    //}
    //open public fun init<K, V>(initialCapacity : Int) : LinkedHashMap<K, V> {
    //val __ = LinkedHashMap(0, null, false)
    //return __
    //}
    //open public fun init<K, V>() : LinkedHashMap<K, V> {
    //val __ = LinkedHashMap(0, null, false)
    //return __
    //}
    //open public fun init<K, V>(m : java.util.Map<out K?, out V?>?) : LinkedHashMap<K, V> {
    //val __ = LinkedHashMap(0, null, false)
    //return __
    //}
    //open public fun init<K, V>(initialCapacity : Int, loadFactor : Float, accessOrder : Boolean) : LinkedHashMap<K, V> {
    //val __ = LinkedHashMap(0, null, false)
    //return __
    //}
    //}
}