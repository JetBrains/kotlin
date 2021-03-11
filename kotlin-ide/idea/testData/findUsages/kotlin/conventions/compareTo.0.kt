// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    infix operator fun <caret>compareTo(other: A): Int = compareTo(other.n)
    infix operator fun compareTo(m: Int): Int = n.compareTo(m)
}

fun test() {
    A(0) compareTo A(1)
    A(0) < A(1)
    A(0) <= A(1)
    A(0) > A(1)
    A(0) >= A(1)
    A(0) compareTo 1
    A(0) < 1
    A(0) <= 1
    A(0) > 1
    A(0) >= 1
}
