// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
fun foo() {
    if (true) {
        fun <caret>bar() {

        }

        bar()
    }

    bar()
}

bar()

// DISABLE-ERRORS