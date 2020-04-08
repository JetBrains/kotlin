// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedClasses
class Outer {
    object O1: A() {

    }

    class Inner {
        object O2: X {

        }
    }
}
