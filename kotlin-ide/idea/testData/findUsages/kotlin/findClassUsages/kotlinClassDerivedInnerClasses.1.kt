// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedClasses
class Outer {
    open class B: A() {

    }

    open class C: Y {

    }

    class Inner {
        interface Z: A {

        }

        class U: Z {

        }
    }
}
