// "Change function signature to 'fun f(a: Int)'" "true"
open class A {
    open fun f(a: Int) {}
}

open class B : A() {
}

class C : B() {
    <caret>override fun f(a: Int) {}
}
