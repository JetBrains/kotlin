package java.util

public trait Iterator<E> {
    fun hasNext() : Boolean
    fun next() : E
    fun remove() : Unit
}