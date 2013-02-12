package kotlin

public class EmptyIterableException(val it : Iterable<*>) : RuntimeException("$it is empty")
