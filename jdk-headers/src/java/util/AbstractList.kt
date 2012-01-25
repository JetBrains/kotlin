package java.util
abstract public open class AbstractList<erased E> protected () : java.util.AbstractCollection<E>(), java.util.List<E> {
    override public fun add(e : E) : Boolean {}
    abstract override public fun get(index : Int) : E
    override public fun set(index : Int, element : E) : E {}
    override public fun add(index : Int, element : E) : Unit {}
    override public fun remove(index : Int) : E {}
    override public fun indexOf(o : Any?) : Int {}
    override public fun lastIndexOf(o : Any?) : Int {}
    override public fun clear() : Unit {}
    override public fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean {}
    override public fun iterator() : java.util.Iterator<E> {}
    override public fun listIterator() : java.util.ListIterator<E> {}
    override public fun listIterator(index : Int) : java.util.ListIterator<E> {}
    override public fun subList(fromIndex : Int, toIndex : Int) : java.util.List<E> {}
//    override public fun equals(o : Any?) : Boolean
//    override public fun hashCode() : Int
    open protected fun removeRange(fromIndex : Int, toIndex : Int) : Unit {}
    protected var modCount : Int = 0
}