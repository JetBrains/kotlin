// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// HIGHLIGHTING
interface A {
    fun test(foo: Int)
}

class B(): A {
    override fun test(foo<caret>: Int) {
        println(foo) // not highlighted `foo`
    }
}
