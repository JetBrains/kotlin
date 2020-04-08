// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
package server

interface Some {
    companion object {
        const val <caret>XX = 1
    }
}

val a = Some.XX