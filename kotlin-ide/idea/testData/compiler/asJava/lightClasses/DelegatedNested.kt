// p.B
package p

class B {
    class A(private val f: I) : I by f {
    }
}

interface I {
    fun g()

    fun f()
}

// LAZINESS:NoLaziness