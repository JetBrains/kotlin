// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIND_BY_REF
// WITH_FILE_NAME
// FIR_IGNORE

package usages

import library.*

fun test() {
    val f = A.T::bar
    A.T().<caret>bar(1)
}