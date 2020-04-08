// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
package server

internal fun <caret>foo() {

}

fun Int.foo() {

}

fun foo(n: Int) {

}

val foo: Int
