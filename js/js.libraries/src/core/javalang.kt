package java.lang

import java.io.IOException
import java.util.Iterator
import js.library

library
trait Iterable<T> {
    fun iterator() : java.util.Iterator<T> = js.noImpl
}

library
open class Exception() : Throwable() {}

library("splitString")
public fun String.split(regex : String) : Array<String> = js.noImpl

library
class IllegalArgumentException(message: String = "") : Exception() {}

library
class IllegalStateException(message: String = "") : Exception() {}

library
class IndexOutOfBoundsException(message: String = "") : Exception() {}

library
class UnsupportedOperationException(message: String = "") : Exception() {}

library
class NumberFormatException(message: String = "") : Exception() {}

public trait Comparable<T> {
    fun compareTo(that: T): Int
}

public trait Appendable {
    open fun append(csq: CharSequence?): Appendable?
    open fun append(csq: CharSequence?, start: Int, end: Int): Appendable?
    open fun append(c: Char): Appendable?
}

