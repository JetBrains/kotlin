package java.util
public open class LinkedList<erased E>(c : java.util.Collection<out E>) : java.util.AbstractSequentialList<E>(),
                                    java.util.List<E>,
                                    java.util.Deque<E>,
                                    java.lang.Cloneable,
                                    java.io.Serializable {
    public this() {}

    override public fun getFirst() : E {}
    override public fun getLast() : E {}
    override public fun removeFirst() : E {}
    override public fun removeLast() : E {}
    override public fun addFirst(e : E) : Unit {}
    override public fun addLast(e : E) : Unit {}
    override public fun contains(o : Any?) : Boolean {}
    override public fun size() : Int {}
    override public fun add(e : E) : Boolean {}
    override public fun remove(o : Any?) : Boolean {}
    override public fun addAll(c : java.util.Collection<out E>) : Boolean {}
    override public fun addAll(index : Int, c : java.util.Collection<out E>) : Boolean {}
    override public fun clear() : Unit {}
    override public fun get(index : Int) : E {}
    override public fun set(index : Int, element : E) : E {}
    override public fun add(index : Int, element : E) : Unit {}
    override public fun remove(index : Int) : E {}
    override public fun indexOf(o : Any?) : Int {}
    override public fun lastIndexOf(o : Any?) : Int {}
    override public fun peek() : E? {}
    override public fun element() : E {}
    override public fun poll() : E? {}
    override public fun remove() : E {}
    override public fun offer(e : E) : Boolean {}
    override public fun offerFirst(e : E) : Boolean {}
    override public fun offerLast(e : E) : Boolean {}
    override public fun peekFirst() : E? {}
    override public fun peekLast() : E? {}
    override public fun pollFirst() : E? {}
    override public fun pollLast() : E? {}
    override public fun push(e : E) : Unit {}
    override public fun pop() : E {}
    override public fun removeFirstOccurrence(o : Any?) : Boolean {}
    override public fun removeLastOccurrence(o : Any?) : Boolean {}
    override public fun listIterator(index : Int) : java.util.ListIterator<E> {}
    override public fun descendingIterator() : java.util.Iterator<E> {}
    override public fun clone() : Any? {}
    override public fun toArray() : Array<Any?> {}
    override public fun toArray<erased T>(a : Array<out T>) : Array<T> {}
//class object {
//open public fun init<E>(c : java.util.Collection<out E?>?) : LinkedList<E> {
//return __
//}
//}
}