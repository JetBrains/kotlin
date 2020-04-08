// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
@file:JvmName("Foo")

@JvmOverloads
fun <caret>foo(
        x: Int = 0,
        y: Double = 0.0,
        z: String = "0"
) {

}