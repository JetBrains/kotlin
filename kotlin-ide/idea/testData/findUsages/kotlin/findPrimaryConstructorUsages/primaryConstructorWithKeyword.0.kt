// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtPrimaryConstructor
// OPTIONS: usages
open class A <caret>constructor(n: Int) {
    constructor(): this(1)
}

class B: A {
    constructor(n: Int): super(n)
}

class C(): A(1)

fun test() {
    A()
    A(1)
}