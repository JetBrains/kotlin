package kotlin

public class EmptyIterableException(val it : Iterable<*>) : RuntimeException("$it is empty")

public class DuplicateKeyException(message : String = "Duplicate keys detected") : RuntimeException(message)