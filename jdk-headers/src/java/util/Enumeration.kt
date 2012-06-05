package java.util
public trait Enumeration<E> {
    open fun hasMoreElements() : Boolean
    open fun nextElement() : E
}