package foo

class A(val ok: String) {
    fun initialize() = ok
}

fun box(): String = A("OK").initialize()

