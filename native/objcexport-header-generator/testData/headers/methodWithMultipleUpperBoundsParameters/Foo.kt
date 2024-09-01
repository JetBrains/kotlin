interface UpperBound
interface UpperBoundA : UpperBound
interface UpperBoundB : UpperBound

class Foo {
    fun <T> bar(t: T) where T : UpperBoundA, T : UpperBoundB = Unit
}
