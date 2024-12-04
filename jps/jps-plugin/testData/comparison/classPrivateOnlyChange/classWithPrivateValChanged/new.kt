package test

class ClassWithPrivateValAdded {
    private val x: Int = 100
    public fun unchangedFun() {}
}

class ClassWithPrivateValRemoved {
    public fun unchangedFun() {}
}

class ClassWithPrivateValSignatureChanged {
    private val x: String = "X"
    public fun unchangedFun() {}
}

class ClassWithGetterForPrivateValChanged {
    private val x: Int
        get() = 200
    public fun unchangedFun() {}
}
