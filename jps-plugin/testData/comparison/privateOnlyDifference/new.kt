package test

class ClassWithPrivateFunAdded {
    public fun main() {}
    private fun privateFun() {}
    val s = java.lang.String.valueOf(20)
}

class ClassWithPrivateFunRemoved {
    public fun main() {}
}

class ClassWithPrivateFunSignatureChanged {
    public fun main() {}
    private fun privateFun(arg: Int) {}
}

class ClassWithPrivateValAdded {
    public fun main() {}
    private val x: Int = 100
}

class ClassWithPrivateValRemoved {
    public fun main() {}
}

class ClassWithPrivateValSignatureChanged {
    public fun main() {}
    private val x: String = "X"
}

class ClassWithPrivateVarAdded {
    public fun main() {}
    private var x: Int = 100
}

class ClassWithPrivateVarRemoved {
    public fun main() {}
}

class ClassWithPrivateVarSignatureChanged {
    public fun main() {}
    private var x: String = "X"
}

class ClassWithGetterForPrivateValChanged {
    public fun main() {}
    private val x: Int
    get() = 200
}

class ClassWithGetterAndSetterForPrivateVarChanged {
    public fun main() {}
    private var x: Int
    get() = 200
    set(value) {}
}


