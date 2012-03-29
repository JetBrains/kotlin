package java.util
public open class LinkedList<erased E>(c : java.util.Collection<out E>) : java.util.AbstractSequentialList<E>(),
                                    java.util.List<E>,
                                    java.util.Deque<E>,
                                    java.lang.Cloneable,
                                    java.io.Serializable {
    public this() {}

    public override fun getFirst() : E {}
    public override fun getLast() : E {}
    public override fun removeFirst() : E {}
    public override fun removeLast() : E {}
    public override fun addFirst(e : E) : Unit {}
    public override fun addLast(e : E) : Unit {}
    public override fun contains(o : Any?) : Boolean {}
    public override fun size() : Int {}
    public override fun add(e : E) : Boolean {}
    public override fun remove(o : Any?) : Boolean {}
    public override fun addAll(c : java.util.Collection<out E>) : Boolean {}
    public override fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean {}
    public override fun clear() : Unit {}
    public override fun get(index : Int) : E {}
    public override fun set(index : Int, element : E) : E {}
    public override fun add(index : Int, element : E) : Unit {}
    public override fun remove(index : Int) : E {}
    public override fun indexOf(o : Any?) : Int {}
    public override fun lastIndexOf(o : Any?) : Int {}
    public override fun peek() : E? {}
    public override fun element() : E {}
    public override fun poll() : E? {}
    public override fun remove() : E {}
    public override fun offer(e : E) : Boolean {}
    public override fun offerFirst(e : E) : Boolean {}
    public override fun offerLast(e : E) : Boolean {}
    public override fun peekFirst() : E? {}
    public override fun peekLast() : E? {}
    public override fun pollFirst() : E? {}
    public override fun pollLast() : E? {}
    public override fun push(e : E) : Unit {}
    public override fun pop() : E {}
    public override fun removeFirstOccurrence(o : Any?) : Boolean {}
    public override fun removeLastOccurrence(o : Any?) : Boolean {}
    public override fun listIterator(index : Int) : java.util.ListIterator<E> {}
    public override fun descendingIterator() : java.util.Iterator<E> {}
    public override fun clone() : Any? {}
    public override fun toArray() : Array<Any?> {}
    public override fun toArray<erased T>(a : Array<out T>) : Array<T> {}
//class object {
//open public fun init<E>(c : java.util.Collection<out E?>?) : LinkedList<E> {
//return __
//}
//}
}