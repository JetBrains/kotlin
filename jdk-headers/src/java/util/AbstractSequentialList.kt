package java.util
abstract public open class AbstractSequentialList<erased E> protected () : java.util.AbstractList<E>() {
    override public fun get(index : Int) : E {}
    override public fun set(index : Int, element : E) : E {}
    override public fun add(index : Int, element : E) : Unit {}
    override public fun remove(index : Int) : E {}
    override public fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean {}
    override public fun iterator() : java.util.Iterator<E> {}
    abstract override public fun listIterator(index : Int) : java.util.ListIterator<E> {}
}