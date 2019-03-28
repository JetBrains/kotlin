package test

open class BaseProtectedConstructor {
    protected fun foo() {

    }
}

internal open class MiddleSamePackage : BaseProtectedConstructor()

internal class DerivedSamePackage : MiddleSamePackage() {
    fun usage() {
        foo()
    }
}
