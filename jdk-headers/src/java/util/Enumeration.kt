package java.util
public trait Enumeration<erased E> {
    open fun hasMoreElements() : Boolean
    open fun nextElement() : E
}