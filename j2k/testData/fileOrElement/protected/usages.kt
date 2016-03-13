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
    init {
        BaseProtectedConstructor().usageInConstructor()
    }

    private val i = BaseProtectedConstructor().usageInPropertyInitializer()

    fun usage() {
        BaseProtectedConstructor().usageInMethod()
    }

    companion object {

        init {
            BaseProtectedConstructor().usageInStaticInit()
        }
    }
}
