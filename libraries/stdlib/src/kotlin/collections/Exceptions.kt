package kotlin

deprecated public class EmptyIterableException(private val it: Iterable<*>) : RuntimeException("$it is empty")

deprecated public class DuplicateKeyException(message : String = "Duplicate keys detected") : RuntimeException(message)
