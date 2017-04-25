// MINIFICATION_THRESHOLD: 544
interface I {
    fun foo() = "OK"
}

class A : I

fun box() = A().foo()