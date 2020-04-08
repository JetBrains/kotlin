// "Change function signature to 'fun f(a: Int)'" "true"
interface A {
    fun f(a: Int)
}

interface B : A {
}

class C : B {
    <caret>override fun f() {}
}
