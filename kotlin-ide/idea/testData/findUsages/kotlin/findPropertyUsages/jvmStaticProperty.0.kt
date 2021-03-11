// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
class Foo {
    companion object {
        @JvmStatic var <caret>foo = 1
    }
}