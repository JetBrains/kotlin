package java.lang

public trait Iterable<erased T> {
    fun iterator() : java.util.Iterator<T>
}