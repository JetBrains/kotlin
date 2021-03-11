package a

import b.B

class A {

    fun bar(b: B) {
        b.foo()
        val a = A().apply {
            b.foo()
            b.foo()
        }
    }
}