// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIR_IGNORE

package anonymousUnused

fun main(args: Array<String>) {
    class LocalClass {
        fun <caret>f() {
        }
    }

    LocalClass().f()
}