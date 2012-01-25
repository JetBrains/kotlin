package java.util
public open class HashSet<erased E>(c : java.util.Collection<out E>) : java.util.AbstractSet<E>(),
                                                                       java.util.Set<E>,
                                                                       java.lang.Cloneable,
                                                                       java.io.Serializable,
                                                                       java.lang.Object {
    public this() {}
    public this(initialCapacity : Int) {}
    public this(initialCapacity : Int, loadFactor : Float) {}
    override public fun iterator() : java.util.Iterator<E> {}
    override public fun size() : Int {}
    override public fun isEmpty() : Boolean {}
    override public fun contains(o : Any?) : Boolean {}
    override public fun add(e : E) : Boolean {}
    override public fun remove(o : Any?) : Boolean {}
    override public fun clear() : Unit {}
    override public fun clone() : Any? {}
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