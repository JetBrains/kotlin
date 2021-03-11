package b

import a.A

class B (val a: A) {
    fun baz(b: B) {
        a.foo(b)
    }
}