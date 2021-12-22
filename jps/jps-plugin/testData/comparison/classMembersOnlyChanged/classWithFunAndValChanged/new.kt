package test

class ClassWithFunAdded {
    fun added() {}
    public fun unchangedFun() {}
}

class ClassWithFunRemoved {
    public fun unchangedFun() {}
}

class ClassWithValAndFunAddedAndRemoved {
    public val valAdded: String = ""
    fun funAdded() {}
    public fun unchangedFun() {}
}

class ClassWithValConvertedToVar {
    public var value: Int = 10
    public fun unchangedFun() {}
}

class ClassWithChangedVisiblityForFun1 {
    private fun foo() {}
    public fun unchangedFun() {}
}

class ClassWithChangedVisiblityForFun2 {
    protected fun foo() {}
    public fun unchangedFun() {}
}
