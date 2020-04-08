// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtSecondaryConstructor
// OPTIONS: usages
open class B {
    internal <caret>constructor() {

    }

    constructor(a: Int): this() {

    }
}

class A : B {
    constructor(a: Int) : super() {

    }

    constructor() {

    }
}

class C : B() {

}

fun test() {
    B()
}