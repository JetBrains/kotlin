package java.lang

import java.util.Iterator;
import js.annotations.library

library
trait Iterable<T> {
    fun iterator() : java.util.Iterator<T> {}
}