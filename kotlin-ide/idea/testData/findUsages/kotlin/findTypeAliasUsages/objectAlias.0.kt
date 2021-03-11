// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtTypeAlias
// OPTIONS: usages
object OOO
typealias <caret>Alias = OOO

fun f() {
    Alias
    val a: Alias
}