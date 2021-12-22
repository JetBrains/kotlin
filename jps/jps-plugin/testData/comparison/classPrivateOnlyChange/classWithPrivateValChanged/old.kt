package test

class ClassWithPrivateValAdded {
    public fun unchangedFun() {}
}

class ClassWithPrivateValRemoved {
    private val x: Int = 100
    public fun unchangedFun() {}
}

class ClassWithPrivateValSignatureChanged {
    private val x: Int = 100
    public fun unchangedFun() {}
}

class ClassWithGetterForPrivateValChanged {
    private val x: Int = 100
    public fun unchangedFun() {}
}
