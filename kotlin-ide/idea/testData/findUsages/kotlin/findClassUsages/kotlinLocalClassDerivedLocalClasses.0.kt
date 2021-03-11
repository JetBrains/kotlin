// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedClasses
fun foo() {
    open class <caret>A

    class B: A()

    interface T: A

    fun bar() {
        class C: A()

        class D: T
    }
}

// DISABLE-ERRORS
