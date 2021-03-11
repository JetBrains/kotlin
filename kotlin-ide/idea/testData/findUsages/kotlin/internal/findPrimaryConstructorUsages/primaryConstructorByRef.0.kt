// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtPrimaryConstructor
// OPTIONS: usages
// FIND_BY_REF
open class A internal constructor(n: Int) {
    constructor(): this(1)
}

class B: A {
    constructor(n: Int): super(n)
}

class C(): A(1)

fun test() {
    A()
    <caret>A(1)
}