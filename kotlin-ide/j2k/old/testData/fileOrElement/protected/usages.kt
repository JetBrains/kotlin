package test

class BaseProtectedConstructor {
    fun usageInConstructor() {

    }

    fun usageInPropertyInitializer(): Int {
        return 1
    }

    fun usageInStaticInit() {

    }

    fun usageInMethod() {

    }
}

internal class DerivedSamePackage {

    private val i = BaseProtectedConstructor().usageInPropertyInitializer()

    init {
        BaseProtectedConstructor().usageInConstructor()
    }

    fun usage() {
        BaseProtectedConstructor().usageInMethod()
    }

    companion object {

        init {
            BaseProtectedConstructor().usageInStaticInit()
        }
    }
}
