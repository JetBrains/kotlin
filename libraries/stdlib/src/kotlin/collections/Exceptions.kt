package kotlin

deprecated("This exception is no longer thrown by any standard library classes and is going to be removed")
public class EmptyIterableException(private val it: Iterable<*>) : RuntimeException("$it is empty")

deprecated("This exception is no longer thrown by any standard library classes and is going to be removed")
public class DuplicateKeyException(message : String = "Duplicate keys detected") : RuntimeException(message)