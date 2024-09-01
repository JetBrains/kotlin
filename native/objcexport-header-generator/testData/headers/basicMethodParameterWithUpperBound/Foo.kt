interface UpperBound

abstract class Foo<T : UpperBound> {
    fun withoutParameter(t: T) = Unit
    fun <Z : T> withParameter(t: Z) = Unit
}