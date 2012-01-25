package java.util
public trait List<erased E> : java.util.Collection<E> {
    override fun size() : Int
    override fun isEmpty() : Boolean
    override fun contains(o : Any?) : Boolean
    override fun iterator() : java.util.Iterator<E>
    override fun toArray() : Array<Any?>
    // Simulate Java's array covariance
    override fun toArray<erased T>(a : Array<out T>) : Array<T>
    override fun add(e : E) : Boolean
    override fun remove(o : Any?) : Boolean
    override fun containsAll(c : java.util.Collection<*>) : Boolean
    override fun addAll(c : java.util.Collection<out E>) : Boolean
    open fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean
    override fun removeAll(c : java.util.Collection<*>) : Boolean
    override fun retainAll(c : java.util.Collection<*>) : Boolean
    override fun clear() : Unit
//    override fun equals(o : Any?) : Boolean
//    override fun hashCode() : Int
    open fun get(index : Int) : E
    open fun set(index : Int, element : E) : E
    open fun add(index : Int, element : E) : Unit
    open fun remove(index : Int) : E
    open fun indexOf(o : Any?) : Int
    open fun lastIndexOf(o : Any?) : Int
    open fun listIterator() : java.util.ListIterator<E>
    open fun listIterator(index : Int) : java.util.ListIterator<E>
    open fun subList(fromIndex : Int, toIndex : Int) : java.util.List<E>
}