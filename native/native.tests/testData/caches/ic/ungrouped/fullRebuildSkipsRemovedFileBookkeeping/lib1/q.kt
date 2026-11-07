var log = ""

fun mark(t: String): String {
    log += t
    return t
}

open class A {
    companion object {
        val p = mark("A")
    }
}

class B : A() {
    companion object {
        val b = mark("B")
    }
}
