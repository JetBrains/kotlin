// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
// FIR_IGNORE

class Outer {
    val x = Outer.t

    companion object {
        private val <caret>t = 1
    }
}

