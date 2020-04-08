// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtSecondaryConstructor
// OPTIONS: usages
// FIND_BY_REF
// WITH_FILE_NAME
package usages

import library.*

class X: A {
    constructor(): super()
}

class Y(): A()

fun test() {
    val a: A = <caret>A()
    val aa = A(1)
}