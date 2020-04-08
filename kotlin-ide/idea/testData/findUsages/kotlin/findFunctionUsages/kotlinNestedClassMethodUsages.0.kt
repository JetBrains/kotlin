// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
package a

public open class Outer() {
    open class Inner {
        fun <caret>foo() {

        }
    }
}
