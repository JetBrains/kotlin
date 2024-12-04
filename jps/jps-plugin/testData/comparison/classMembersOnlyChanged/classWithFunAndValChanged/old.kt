package test

class ClassWithFunAdded {
    public fun unchangedFun() {}
}

class ClassWithFunRemoved {
    fun removed() {}
    public fun unchangedFun() {}
}

class ClassWithValAndFunAddedAndRemoved {
    public val valRemoved: Int = 10
    fun funRemoved() {}
    public fun unchangedFun() {}
}

class ClassWithValConvertedToVar {
    public val value: Int = 10
    public fun unchangedFun() {}
}

class ClassWithChangedVisiblityForFun1 {
    protected fun foo() {}
    public fun unchangedFun() {}
}

class ClassWithChangedVisiblityForFun2 {
    private fun foo() {}
    public fun unchangedFun() {}
}


