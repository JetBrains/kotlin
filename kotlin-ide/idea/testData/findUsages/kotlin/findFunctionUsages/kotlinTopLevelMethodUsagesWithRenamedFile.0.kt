// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
@file:JvmName("RequestProcessor")
package server

fun <caret>processRequest() = "foo"

