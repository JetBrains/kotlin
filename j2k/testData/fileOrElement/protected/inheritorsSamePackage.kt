package test

open class BaseInheritorSamePackage protected constructor() {

    protected fun foo() {

    }

    protected var i = 1
}

internal class DerivedInheritorSamePackage : BaseInheritorSamePackage() {
    fun usage1() {
        val base = BaseInheritorSamePackage()
        base.foo()
        val i = base.i
    }
}