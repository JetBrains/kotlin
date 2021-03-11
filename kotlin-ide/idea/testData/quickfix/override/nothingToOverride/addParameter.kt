// "Change function signature to 'fun f(a: Int)'" "true"
interface A {
    fun f(a: Int)
}

class B : A {
    <caret>override fun f() {}
}
