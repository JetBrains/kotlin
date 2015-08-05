package test

class ClassWithPrivateVarAdded {
    private var x: Int = 100
    public fun unchangedFun() {}
}

class ClassWithPrivateVarRemoved {
    public fun unchangedFun() {}
}

class ClassWithPrivateVarSignatureChanged {
    private var x: String = "X"
    public fun unchangedFun() {}
}

class ClassWithGetterAndSetterForPrivateVarChanged {
    private var x: Int
    get() = 200
    set(value) {}
    public fun unchangedFun() {}
}
