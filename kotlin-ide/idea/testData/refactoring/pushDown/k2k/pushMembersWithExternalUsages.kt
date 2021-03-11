open class <caret>A {
    fun foo() {

    }

    // INFO: {"checked": "true"}
    fun foo(n: Int) {

    }

    fun test() {
        foo()
        foo(1)
    }
}

open class B : A() {

}

class C : B() {

}

fun A.test() {
    foo()
    foo(2)
}

fun test() {
    A().foo()
    A().foo(3)
    B().foo()
    B().foo(4)
    C().foo()
    C().foo(5)
}