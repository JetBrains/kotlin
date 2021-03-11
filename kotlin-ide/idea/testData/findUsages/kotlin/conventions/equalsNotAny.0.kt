// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// CHECK_SUPER_METHODS_YES_NO_DIALOG: no
// OPTIONS: usages
// FIND_BY_REF

class A(val n: Int) {
    fun foo(a: A) = a <caret>== this

    override infix fun equals(other: Any?): Boolean = TODO()
}

fun test() {
    1 == 2
    A(0) == A(1)
    A(0) != A(1)
    A(0) equals A(1)
    A(0) === A(1)
    A(0) !== A(1)
}
