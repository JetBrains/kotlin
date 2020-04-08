open class A {
    open fun foo(f: (Int, <caret>Boolean) -> String) {

    }
}

class B : A() {
    override fun foo(f: (Int, Boolean) -> String) {

    }
}