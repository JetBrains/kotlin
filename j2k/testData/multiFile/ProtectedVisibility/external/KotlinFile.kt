package test3

import test.*

class DerivedOtherPackageKotlin : BaseOtherPackage() {
    fun usage1() {
        val base = BaseOtherPackage()
        base.foo()
        val i = base.i
    }
}