// MINIFICATION_THRESHOLD: 548
interface I {
    fun foo() = "OK"
}

interface J : I

class A : J

fun box() = A().foo()