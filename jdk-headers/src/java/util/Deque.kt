package java.util
public trait Deque<erased E> : java.util.Queue<E> {
    open fun addFirst(e : E) : Unit
    open fun addLast(e : E) : Unit
    open fun offerFirst(e : E) : Boolean
    open fun offerLast(e : E) : Boolean
    open fun removeFirst() : E
    open fun removeLast() : E
    open fun pollFirst() : E?
    open fun pollLast() : E?
    open fun getFirst() : E
    open fun getLast() : E
    open fun peekFirst() : E?
    open fun peekLast() : E?
    open fun removeFirstOccurrence(o : Any?) : Boolean
    open fun removeLastOccurrence(o : Any?) : Boolean
    override fun add(e : E) : Boolean
    override fun offer(e : E) : Boolean
    override fun remove() : E
    override fun poll() : E?
    override fun element() : E
    override fun peek() : E?
    open fun push(e : E) : Unit
    open fun pop() : E
    override fun remove(o : Any?) : Boolean
    override fun contains(o : Any?) : Boolean
    public override fun size() : Int
    override fun iterator() : java.util.Iterator<E>
    open fun descendingIterator() : java.util.Iterator<E>
}