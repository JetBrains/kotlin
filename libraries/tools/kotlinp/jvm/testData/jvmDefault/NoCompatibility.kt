// JVM_DEFAULT_MODE: no-compatibility

interface A {
    fun f() {}
    fun g()
}

interface B : A {
    override fun g() {}
}

class C : B
