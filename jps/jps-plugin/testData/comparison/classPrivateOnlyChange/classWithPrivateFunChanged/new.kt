package test

class ClassWithPrivateFunAdded {
    private fun privateFun() {}
    val s = "20"
}

class ClassWithPrivateFunRemoved {
    public fun unchangedFun() {}
}

class ClassWithPrivateFunSignatureChanged {
    private fun privateFun(arg: Int) {}
    public fun unchangedFun() {}
}
