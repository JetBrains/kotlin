class A {
    fun f(param: Int)
}

interface B {
    fun f(p: Int)
}

class C : A, B {
    override fun <caret>f(p: Int) {}
}