open class A<T, U> {
}

class <caret>B<X>: A<String, X>() {
    // INFO: {"checked": "true"}
    fun foo(s: String, x: X) {

    }
}

class C<X>: A<Int, B<X>>() {
    fun foo(s: String, x: B<X>) {

    }
}

class D<X>: A<Int, X>() {
    fun foo(s: String, x: B<X>) {

    }
}