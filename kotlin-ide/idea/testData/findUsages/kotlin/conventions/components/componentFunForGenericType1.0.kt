// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtFunction
// OPTIONS: usages

class X<T>

operator fun <T> X<T>.<caret>component1(): Int = 0
operator fun <T> X<T>.component2(): Int = 0

fun f() = X<String>()

fun test() {
    val (x, y) = f()
}
