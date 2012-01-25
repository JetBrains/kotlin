package java.util
public trait Queue<erased E> : java.util.Collection<E> {
    override fun add(e : E) : Boolean
    open fun offer(e : E) : Boolean
    open fun remove() : E
    open fun poll() : E?
    open fun element() : E
    open fun peek() : E?
}