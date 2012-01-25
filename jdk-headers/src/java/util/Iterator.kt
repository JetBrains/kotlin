package java.util

public trait Iterator<erased E> {
    fun hasNext() : Boolean
    fun next() : E
    fun remove() : Unit
}