// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

fun <R> <caret>foo(f: () -> R) = f()

fun test() {
    foo {
        return@foo false
    }

    foo(fun(): Boolean { return@foo false })
}