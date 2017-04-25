// MINIFICATION_THRESHOLD: 544
interface I {
    val foo: String
        get() = "OK"
}

class A : I

fun box() = A().foo