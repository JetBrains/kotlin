// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedClasses

fun foo() {
    interface Z: A {

    }

    object O1: A()

    fun bar() {
        object O2: Z
    }
}
