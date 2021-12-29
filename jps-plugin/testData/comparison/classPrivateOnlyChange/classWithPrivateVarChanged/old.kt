package test

class ClassWithPrivateVarAdded {
    public fun unchangedFun() {}
}

class ClassWithPrivateVarRemoved {
    private var x: Int = 100
    public fun unchangedFun() {}
}

class ClassWithPrivateVarSignatureChanged {
    private var x: Int = 100
    public fun unchangedFun() {}
}

class ClassWithGetterAndSetterForPrivateVarChanged {
    private var x: Int = 100
    public fun unchangedFun() {}
}
