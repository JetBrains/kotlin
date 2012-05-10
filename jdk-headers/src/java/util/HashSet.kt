package java.util
public open class HashSet<erased E>(c : java.util.Collection<out E>) : java.util.AbstractSet<E>(),
                                                                       java.util.Set<E>,
                                                                       java.lang.Cloneable,
                                                                       java.io.Serializable {
    public this() {}
    public this(initialCapacity : Int) {}
    public this(initialCapacity : Int, loadFactor : Float) {}
    public override fun iterator() : java.util.Iterator<E> {}
    public override fun size() : Int {}
    public override fun isEmpty() : Boolean {}
    public override fun contains(o : Any?) : Boolean {}
    public override fun add(e : E) : Boolean {}
    public override fun remove(o : Any?) : Boolean {}
    public override fun clear() : Unit {}
    public override fun clone() : Any? {}
//class object {
//open public fun init<E>() : HashSet<E> {
//val __ = HashSet(0, null, null)
//return __
//}
//open public fun init<E>(c : java.util.Collection<out E?>?) : HashSet<E> {
//val __ = HashSet(0, null, null)
//return __
//}
//open public fun init<E>(initialCapacity : Int, loadFactor : Float) : HashSet<E> {
//val __ = HashSet(0, null, null)
//return __
//}
//open public fun init<E>(initialCapacity : Int) : HashSet<E> {
//val __ = HashSet(0, null, null)
//return __
//}
//open fun init<E>(initialCapacity : Int, loadFactor : Float, dummy : Boolean) : HashSet<E> {
//val __ = HashSet(0, null, null)
//return __
//}
//}
}