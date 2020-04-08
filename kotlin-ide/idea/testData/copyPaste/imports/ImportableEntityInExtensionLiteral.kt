package a

import a.A.Inner
import a.A.Nested

class A {
    inner class Inner {
    }
    class Nested {
    }
    fun foo() {
    }
}

fun A.ext() {
}

fun f(body: A.() -> Unit) {
}

<selection>fun g() {
    f {
        Inner()
        Nested()
        foo()
        ext()
    }
}</selection>