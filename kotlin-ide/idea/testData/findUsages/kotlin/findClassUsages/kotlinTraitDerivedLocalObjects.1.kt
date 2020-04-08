// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedClasses

fun foo() {
    object O1: A() {

    }

    fun bar() {
        object O2: X {

        }
    }
}
