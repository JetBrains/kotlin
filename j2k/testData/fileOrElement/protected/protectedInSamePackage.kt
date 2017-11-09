package test

class BaseSamePackage {

    var i = 1

    fun foo() {

    }
}

internal class DerivedSamePackage {
    fun usage1() {
        val base = BaseSamePackage()
        base.foo()
        val i = base.i
    }
}
