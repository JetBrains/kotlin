// FLOW: OUT
// WITH_DEREFERENCES

class A

operator fun A.get(i: Int) = this
operator fun A.set(i: Int, a: A) = this
operator fun A.plusAssign(a: A) = this
operator fun A.times(a: A) = this
operator fun A.inc() = this

fun test() {
    val <caret>x = A()
    val y = A()

    x[1]
    x[1] = y
    x[1] += y
    x[1] *= y
    x[1]++
}