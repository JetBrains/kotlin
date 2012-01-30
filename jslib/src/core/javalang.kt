package java.lang

import java.util.Iterator;
import js.annotations.LibraryClass

LibraryClass
trait Iterable<T> {
    fun iterator() : java.util.Iterator<T> {}
}