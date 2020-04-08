// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages
// FIND_BY_REF
// WITH_FILE_NAME
package usages

import library.*

class X: A {
    constructor(n: Int): super(n)
}

class Y(): A(1)

fun test() {
    val a: <caret>A = A()
    val aa = A(1)
}