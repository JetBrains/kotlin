// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

class A(var n: Int) {
    operator fun <caret>plusAssign(m: Int) {
        n += m
    }

    operator fun plusAssign(a: A) {
        this += a.n
    }
}

fun test() {
    val a = A(0)
    a.plusAssign(1)
    a += 1
    a += A(1)
}
