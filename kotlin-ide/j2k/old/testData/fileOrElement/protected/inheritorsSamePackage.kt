package test

open class BaseInheritorSamePackage {

    var i = 1

    constructor() {

    }

    protected constructor(x: Int) {

    }

    fun foo() {
        BaseInheritorSamePackage(1)
    }
}

internal class DerivedInheritorSamePackage : BaseInheritorSamePackage() {
    fun usage1() {
        val base = BaseInheritorSamePackage()
        base.foo()
        val i = base.i
    }
}