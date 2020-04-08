// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages

data class A(val <caret>a: A?, val n: Int)

fun f(a: A) {
    val (a1, n1) = a
    val (a2, n2) =
            a?.a ?: return
}