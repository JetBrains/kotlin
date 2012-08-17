package java.lang

import java.io.IOException
import js.library

library
open public class Exception(message: String? = null) : Throwable() {}

library("splitString")
public fun String.split(regex : String) : Array<String> = js.noImpl

library
public class IllegalArgumentException(message: String? = null) : Exception() {}

library
public class IllegalStateException(message: String? = null) : Exception() {}

library
public class IndexOutOfBoundsException(message: String? = null) : Exception() {}

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
    public open fun append(csq: CharSequence?): Appendable?
    public open fun append(csq: CharSequence?, start: Int, end: Int): Appendable?
    public open fun append(c: Char): Appendable?
}
