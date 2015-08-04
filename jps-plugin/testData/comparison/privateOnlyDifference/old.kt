package test

class ClassWithPrivateFunAdded {
    public fun main() {}
    val s = java.lang.String.valueOf(20)
}

class ClassWithPrivateFunRemoved {
    public fun main() {}
    private fun privateFun() {}
}

class ClassWithPrivateFunSignatureChanged {
    public fun main() {}
    private fun privateFun(arg: String) {}
}

class ClassWithPrivateValAdded {
    public fun main() {}
}

class ClassWithPrivateValRemoved {
    public fun main() {}
    private val x: Int = 100
}

class ClassWithPrivateValSignatureChanged {
    public fun main() {}
    private val x: Int = 100
}

class ClassWithPrivateVarAdded {
    public fun main() {}
}

class ClassWithPrivateVarRemoved {
    public fun main() {}
    private var x: Int = 100
}

class ClassWithPrivateVarSignatureChanged {
    public fun main() {}
    private var x: Int = 100
}

class ClassWithGetterForPrivateValChanged {
    public fun main() {}
    private val x: Int = 100
}

class ClassWithGetterAndSetterForPrivateVarChanged {
    public fun main() {}
    private var x: Int = 100
}




