package test

class ClassWithPrivateFunAdded {
    val s = "20"
}

class ClassWithPrivateFunRemoved {
    private fun privateFun() {}
    public fun unchangedFun() {}
}

class ClassWithPrivateFunSignatureChanged {
    private fun privateFun(arg: String) {}
    public fun unchangedFun() {}
}
