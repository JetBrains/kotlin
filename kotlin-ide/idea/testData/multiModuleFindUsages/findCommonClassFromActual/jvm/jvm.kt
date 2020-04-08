// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, expected

actual class <caret>My(val s: String) {
    actual fun boo() {}
}
