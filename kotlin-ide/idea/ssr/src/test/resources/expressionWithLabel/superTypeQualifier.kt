interface B {
    fun foo() {}
}

interface C {
    fun foo() {}
}

class A : B, C {
    override fun foo() {
        <warning descr="SSR">super<B></warning>.foo()
        super<C>.foo()
    }
}

fun main() { A().foo() }