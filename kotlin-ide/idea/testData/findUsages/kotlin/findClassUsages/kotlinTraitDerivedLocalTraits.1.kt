// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedInterfaces

fun foo() {
    open class B: A() {

    }

    fun bar() {
        interface Z: A {

        }

        interface U: Z {

        }
    }
}
