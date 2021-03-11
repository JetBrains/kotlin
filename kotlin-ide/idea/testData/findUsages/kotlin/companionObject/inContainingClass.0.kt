// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtObjectDeclaration
// OPTIONS: usages
class A() {
    init {
        foo()
        v

        1[2] // using companion object function by convention

        ext() // companion object is extension receiver
    }

    companion <caret>object: Foo {
        fun foo() {
        }

        val v = 42

        fun Int.get(a: Int) = this + a
    }
}

interface Foo

fun Foo.ext() {
}

// DISABLE-ERRORS