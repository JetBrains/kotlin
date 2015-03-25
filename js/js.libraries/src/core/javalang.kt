package java.lang

library
open public class Exception(message: String? = null): Throwable(message) {}

library
open public class RuntimeException(message: String? = null) : Exception(message) {}

library
public class IllegalArgumentException(message: String? = null) : RuntimeException(message) {}

library
public class IllegalStateException(message: String? = null) : RuntimeException(message) {}

library
public class IndexOutOfBoundsException(message: String? = null) : RuntimeException(message) {}

library
public class UnsupportedOperationException(message: String? = null) : RuntimeException(message) {}

library
public class NumberFormatException(message: String? = null) : RuntimeException(message) {}

library
public class NullPointerException(message: String? = null) : RuntimeException(message) {}

library
public trait Runnable {
    public open fun run() : Unit;
}

library
public trait Appendable {
    public open fun append(csq: CharSequence?): Appendable
    public open fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    public open fun append(c: Char): Appendable
}

library
public class StringBuilder(capacity: Int? = null) : Appendable {
    override fun append(c: Char): StringBuilder = noImpl
    override fun append(csq: CharSequence?): StringBuilder = noImpl
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder = noImpl
    public fun append(obj: Any?): StringBuilder = noImpl
    public fun reverse(): StringBuilder = noImpl
    override fun toString(): String = noImpl
}
