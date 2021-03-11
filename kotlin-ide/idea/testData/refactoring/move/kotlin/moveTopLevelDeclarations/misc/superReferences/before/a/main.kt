package a

open class A {
    fun foo() {}
}

open class <caret>B : A() {
    fun t() {
        super.foo()
    }
}