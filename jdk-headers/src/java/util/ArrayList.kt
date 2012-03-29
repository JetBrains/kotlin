package java.util
public open class ArrayList<erased E>(c : java.util.Collection<out E>) : java.util.AbstractList<E>(),
                                                        java.util.List<E>,
                                                        java.util.RandomAccess,
                                                        java.lang.Cloneable,
                                                        java.io.Serializable {
    public this() {}
    public this(initialCapacity : Int) {}
    open public fun trimToSize() : Unit {}
    open public fun ensureCapacity(minCapacity : Int) : Unit {}
    public override fun size() : Int {}
    public override fun isEmpty() : Boolean {}
    public override fun contains(o : Any?) : Boolean {}
    public override fun indexOf(o : Any?) : Int {}
    public override fun lastIndexOf(o : Any?) : Int {}
    public override fun clone() : java.lang.Object {}
    public override fun toArray() : Array<Any?> {}
    public override fun toArray<erased T>(a : Array<out T>) : Array<T> {}
    public override fun get(index : Int) : E {}
    public override fun set(index : Int, element : E) : E {}
    public override fun add(e : E) : Boolean {}
    public override fun add(index : Int, element : E) : Unit {}
    public override fun remove(index : Int) : E {}
    public override fun remove(o : Any?) : Boolean {}
    public override fun clear() : Unit {}
    public override fun addAll(c : java.util.Collection<out E>) : Boolean {}
    public override fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean {}
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
