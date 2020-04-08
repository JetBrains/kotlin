// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtFunction
// OPTIONS: usages

class X<T>

operator fun X<Int>.<caret>component1(): Int = 0
operator fun X<Int>.component2(): Int = 0

operator fun X<String>.component1(): Int = 0
operator fun X<String>.component2(): Int = 0

fun f() = X<Int>()
fun g() = X<String>()

fun test() {
    val (x1, y1) = f()
    val (x2, y2) = g()
}
