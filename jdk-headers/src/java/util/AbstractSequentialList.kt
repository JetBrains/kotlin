package java.util
public abstract class AbstractSequentialList<erased E> protected () : java.util.AbstractList<E>() {
    public override fun get(index : Int) : E {}
    public override fun set(index : Int, element : E) : E {}
    public override fun add(index : Int, element : E) : Unit {}
    public override fun remove(index : Int) : E {}
    public override fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean {}
    public override fun iterator() : java.util.Iterator<E> {}
    public abstract override fun listIterator(index : Int) : java.util.ListIterator<E> {}
}