interface UpperBound
interface UpperBoundA : UpperBound
interface UpperBoundB : UpperBound

class Foo {
    fun <A : UpperBoundA, B : UpperBoundB> bar(a: A): B = null!!
    fun <A : UpperBoundA, B : UpperBoundB> bar(b: B): A = null!!
}