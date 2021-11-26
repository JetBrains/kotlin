// !JVM_DEFAULT_MODE: all

interface A {
    fun f() {}
    fun g()
}

interface B : A {
    override fun g() {}
}

class C : B
