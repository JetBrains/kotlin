// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedInterfaces

fun foo() {
    open class B: A() {

    }

    open class C: Y {

    }

    fun bar() {
        interface Z: A {

        }

        interface U: Z {

        }
    }
}
