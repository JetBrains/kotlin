package java.lang

@library
open public class Error(message: String? = null) : Throwable(message) {}

@library
open public class Exception(message: String? = null) : Throwable(message) {}

@library
open public class RuntimeException(message: String? = null) : Exception(message) {}

@library
public class IllegalArgumentException(message: String? = null) : RuntimeException(message) {}

@library
public class IllegalStateException(message: String? = null) : RuntimeException(message) {}

@library
public class IndexOutOfBoundsException(message: String? = null) : RuntimeException(message) {}

@library
public class UnsupportedOperationException(message: String? = null) : RuntimeException(message) {}

@library
public class NumberFormatException(message: String? = null) : RuntimeException(message) {}

@library
public class NullPointerException(message: String? = null) : RuntimeException(message) {}

@library
public class AssertionError(message: String? = null) : Error(message) {}

@library
public interface Runnable {
    public open fun run(): Unit
}

public fun Runnable(action: () -> Unit): Runnable = object : Runnable {
    override fun run() = action()
}

@library
public interface Appendable {
    public open fun append(csq: CharSequence?): Appendable
    public open fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    public open fun append(c: Char): Appendable
}

@library
public class StringBuilder(content: String = "") : Appendable, CharSequence {
    override val length: Int = noImpl
    override fun get(index: Int): Char = noImpl
    override fun subSequence(start: Int, end: Int): CharSequence = noImpl
    override fun append(c: Char): StringBuilder = noImpl
    override fun append(csq: CharSequence?): StringBuilder = noImpl
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder = noImpl
    public fun append(obj: Any?): StringBuilder = noImpl
    public fun reverse(): StringBuilder = noImpl
    override fun toString(): String = noImpl
}

public inline fun StringBuilder(capacity: Int): StringBuilder = StringBuilder()
public inline fun StringBuilder(content: CharSequence): StringBuilder = StringBuilder(content.toString())
