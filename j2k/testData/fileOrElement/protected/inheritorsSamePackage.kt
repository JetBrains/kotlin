package test

open class BaseInheritorSamePackage {
    constructor() {

    }

    protected constructor(x: Int) {

    }

    fun foo() {
        BaseInheritorSamePackage(1)
    }

    var i = 1
}

internal class DerivedInheritorSamePackage : BaseInheritorSamePackage() {
    fun usage1() {
        val base = BaseInheritorSamePackage()
        base.foo()
        val i = base.i
    }
}