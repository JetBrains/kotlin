package test

class BaseProtectedConstructor {
    fun usageInConstructor() {}
    fun usageInPropertyInitializer(): Int {
        return 1
    }

    fun usageInStaticInit() {}
    fun usageInMethod() {}
}

internal class DerivedSamePackage {
    private val i = BaseProtectedConstructor().usageInPropertyInitializer()

    companion object {init {
        BaseProtectedConstructor().usageInStaticInit()
    }
    }

    fun usage() {
        BaseProtectedConstructor().usageInMethod()
    }

    init {
        BaseProtectedConstructor().usageInConstructor()
    }
}
