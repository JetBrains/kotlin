// PROBLEM: none
// WITH_RUNTIME
class A(val _value: Int) {
    operator fun compareTo(other: Int) = _value.compareTo(other)
}

fun test(a: A): Boolean {
    return a >= 0 && a <= 100<caret>
}