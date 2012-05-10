package java.util
public abstract class AbstractList<erased E> protected () : java.util.AbstractCollection<E>(), java.util.List<E> {
    public override fun add(e : E) : Boolean {}
    public abstract override fun get(index : Int) : E
    public override fun set(index : Int, element : E) : E {}
    public override fun add(index : Int, element : E) : Unit {}
    public override fun remove(index : Int) : E {}
    public override fun indexOf(o : Any?) : Int {}
    public override fun lastIndexOf(o : Any?) : Int {}
    public override fun clear() : Unit {}
    public override fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean {}
    public override fun iterator() : java.util.Iterator<E> {}
    public override fun listIterator() : java.util.ListIterator<E> {}
    public override fun listIterator(index : Int) : java.util.ListIterator<E> {}
    public override fun subList(fromIndex : Int, toIndex : Int) : java.util.List<E> {}
//    public override fun equals(o : Any?) : Boolean
//    public override fun hashCode() : Int
    open protected fun removeRange(fromIndex : Int, toIndex : Int) : Unit {}
    protected var modCount : Int = 0
}