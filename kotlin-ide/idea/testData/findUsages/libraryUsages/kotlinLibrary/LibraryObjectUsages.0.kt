// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtObjectDeclaration
// OPTIONS: usages
// FIND_BY_REF
// WITH_FILE_NAME
// FIR_IGNORE

package usages

import library.*

fun test() {
    val o = <caret>O
}