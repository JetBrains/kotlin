package kotlin

public class EmptyIterableException(private val it: Iterable<*>) : RuntimeException("$it is empty")

public class DuplicateKeyException(message : String = "Duplicate keys detected") : RuntimeException(message)