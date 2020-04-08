// WITH_RUNTIME
class A(val _value: Int): Comparable<A> {
    override operator fun compareTo(other: A) = _value.compareTo(other._value)
}

val low = A(0)
val high = A(100)
fun test(a: A): Boolean {
    return low <= a && a <= high<caret>
}