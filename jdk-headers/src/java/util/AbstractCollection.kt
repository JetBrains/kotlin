package java.util
abstract public open class AbstractCollection<erased E> protected () : java.lang.Object(), java.util.Collection<E> {
    abstract override public fun iterator() : java.util.Iterator<E>
    abstract override public fun size() : Int
    override public fun isEmpty() : Boolean {}
    override public fun contains(o : Any?) : Boolean {}
    override public fun toArray() : Array<Any?> {}
    override public fun toArray<erased T>(a : Array<out T>) : Array<T> {}
    override public fun add(e : E) : Boolean {}
    override public fun remove(o : Any?) : Boolean {}
    override public fun containsAll(c : java.util.Collection<*>) : Boolean {}
    override public fun addAll(c : java.util.Collection<out E>) : Boolean {}
    override public fun removeAll(c : java.util.Collection<*>) : Boolean {}
    override public fun retainAll(c : java.util.Collection<*>) : Boolean {}
    override public fun clear() : Unit {}
//override public fun toString() : java.lang.String {}
}
