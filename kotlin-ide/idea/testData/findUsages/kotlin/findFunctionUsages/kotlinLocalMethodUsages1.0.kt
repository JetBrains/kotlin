// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIR_IGNORE

fun foo() {
    fun <caret>bar() {

    }

    bar()
}

bar()
