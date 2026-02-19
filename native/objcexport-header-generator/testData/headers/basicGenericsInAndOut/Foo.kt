interface UpperBound

class Foo<out T : UpperBound> {
    fun bar(): T = null!!
}

class Bar<in T : UpperBound> {
    fun foo(t: T) = Unit
}