// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages
// FIND_BY_REF
// WITH_FILE_NAME
package usages

import library.*

class X: A.T {
    constructor(n: Int): super(n)
}

class Y(): A.T(1)

fun test() {
    val a: A.<caret>T = A.T()
    val aa = A.T(1)
}