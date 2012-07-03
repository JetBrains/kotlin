package java.lang

import java.io.IOException
import java.util.Iterator
import js.library

library
public trait Iterable<T> {
    public fun iterator() : java.util.Iterator<T> = js.noImpl
}

library
open public class Exception() : Throwable() {}

library("splitString")
public fun String.split(regex : String) : Array<String> = js.noImpl

library
public class IllegalArgumentException(message: String = "") : Exception() {}

library
public class IllegalStateException(message: String = "") : Exception() {}

library
public class IndexOutOfBoundsException(message: String = "") : Exception() {}

library
public class UnsupportedOperationException(message: String = "") : Exception() {}

library
public class NumberFormatException(message: String = "") : Exception() {}

library
public trait Runnable {
    public open fun run() : Unit;
}

public trait Comparable<T> {
    public fun compareTo(that: T): Int
}

public trait Appendable {
    public open fun append(csq: CharSequence?): Appendable?
    public open fun append(csq: CharSequence?, start: Int, end: Int): Appendable?
    public open fun append(c: Char): Appendable?
}
