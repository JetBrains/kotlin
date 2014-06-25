package java.lang

import java.io.IOException
import js.library

native("Error")
open public class Exception(message: String? = null): Throwable() {}

library
open public class RuntimeException(message: String? = null) : Exception(message) {}

library
public class IllegalArgumentException(message: String? = null) : Exception() {}

library
public class IllegalStateException(message: String? = null) : Exception() {}

native("RangeError")
public class IndexOutOfBounds(message: String? = null) : Exception(message) {}

native("RangeError")
public class IndexOutOfBoundsException(message: String? = null) : Exception(message) {}

library
public class UnsupportedOperationException(message: String? = null) : Exception() {}

library
public class NumberFormatException(message: String? = null) : Exception() {}

library
public trait Runnable {
    public open fun run() : Unit;
}

library
public trait Comparable<T> {
    public fun compareTo(that: T): Int
}

library
public trait Appendable {
    public open fun append(csq: CharSequence?): Appendable
    public open fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    public open fun append(c: Char): Appendable
}

library
public class StringBuilder() : Appendable {
    override fun append(c: Char): StringBuilder = js.noImpl
    override fun append(csq: CharSequence?): StringBuilder = js.noImpl
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder = js.noImpl
    public fun append(obj: Any?): StringBuilder = js.noImpl
    public fun reverse(): StringBuilder = js.noImpl
    override fun toString(): String = js.noImpl
}
