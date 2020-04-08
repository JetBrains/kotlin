// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
class C

operator fun C.plusAssign(p: Int) = this

fun foo() {
    val <caret>c = C()
    c += 10
}
