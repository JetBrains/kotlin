// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtObjectDeclaration
package a

class A {
    object <caret>O {
        var foo: String = "foo"
    }
}
