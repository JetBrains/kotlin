package java.util
public trait Collection<erased E> : java.lang.Iterable<E> {
    open fun size() : Int
    open fun isEmpty() : Boolean
    open fun contains(o : Any?) : Boolean
    override fun iterator() : java.util.Iterator<E>
    open fun toArray() : Array<Any?>
    // a : Array<out T> to emulate Java's array covariance
    open fun toArray<erased T>(a : Array<out T>) : Array<T>
    open fun add(e : E) : Boolean
    open fun remove(o : Any?) : Boolean
    open fun containsAll(c : java.util.Collection<*>) : Boolean
    open fun addAll(c : java.util.Collection<out E>) : Boolean
    open fun removeAll(c : java.util.Collection<*>) : Boolean
    open fun retainAll(c : java.util.Collection<*>) : Boolean
    open fun clear() : Unit

//    override fun equals(o : Any?) : Boolean
//    override fun hashCode() : Int
}