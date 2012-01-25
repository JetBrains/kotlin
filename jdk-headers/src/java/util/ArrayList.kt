package java.util
public open class ArrayList<erased E>(c : java.util.Collection<out E>) : java.util.AbstractList<E>(),
                                                        java.util.List<E>,
                                                        java.util.RandomAccess,
                                                        java.lang.Cloneable,
                                                        java.io.Serializable,
                                                        java.lang.Object {
    public this() {}
    public this(initialCapacity : Int) {}
    open public fun trimToSize() : Unit {}
    open public fun ensureCapacity(minCapacity : Int) : Unit {}
    override public fun size() : Int {}
    override public fun isEmpty() : Boolean {}
    override public fun contains(o : Any?) : Boolean {}
    override public fun indexOf(o : Any?) : Int {}
    override public fun lastIndexOf(o : Any?) : Int {}
    override public fun clone() : java.lang.Object {}
    override public fun toArray() : Array<Any?> {}
    override public fun toArray<erased T>(a : Array<out T>) : Array<T> {}
    override public fun get(index : Int) : E {}
    override public fun set(index : Int, element : E) : E {}
    override public fun add(e : E) : Boolean {}
    override public fun add(index : Int, element : E) : Unit {}
    override public fun remove(index : Int) : E {}
    override public fun remove(o : Any?) : Boolean {}
    override public fun clear() : Unit {}
    override public fun addAll(c : java.util.Collection<out E>) : Boolean {}
    override public fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean {}
    override protected fun removeRange(fromIndex : Int, toIndex : Int) : Unit {}
//    class object {
//        open public fun init<E>() : ArrayList<E> {
//            return __
//        }
//        open public fun init<E>(c : java.util.Collection<out E?>?) : ArrayList<E> {
//            return __
//        }
//    }
}