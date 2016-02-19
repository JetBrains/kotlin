package test

internal open class BaseSuperSamePackage {
    fun usage1() {
        val derived = DerivedSuperSamePackage()
        derived.foo()
        val i = derived.i
    }
}

internal class DerivedSuperSamePackage : BaseSuperSamePackage() {

    fun foo() {

    }

    var i = 1
}