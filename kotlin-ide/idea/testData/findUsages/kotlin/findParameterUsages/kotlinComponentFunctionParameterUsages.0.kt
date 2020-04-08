// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages
package test

public data class KotlinDataClass(val <caret>foo: Int, val bar: String) {
}