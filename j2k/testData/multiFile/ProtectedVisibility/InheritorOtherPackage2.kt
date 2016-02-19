package test2

import test.*

class DerivedOtherPackage : BaseOtherPackage() {
    fun usage1() {
        val base = BaseOtherPackage()
        base.foo()
        val i = base.i
    }
}