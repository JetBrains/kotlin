// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtObjectDeclaration
// OPTIONS: usages, constructorUsages
// FIND_BY_REF
// WITH_FILE_NAME
package usages

import library.*

fun test() {
    val a = A.<caret>Companion
}