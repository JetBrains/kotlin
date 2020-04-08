// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
fun foo() {
    fun <caret>bar() {

    }

    bar()
}

bar()
