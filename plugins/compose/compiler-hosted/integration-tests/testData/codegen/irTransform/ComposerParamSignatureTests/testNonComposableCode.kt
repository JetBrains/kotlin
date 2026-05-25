fun A() {}
val b: Int get() = 123
fun C(x: Int) {
    var x = 0
    x++

    class D {
        fun E() { A() }
        val F: Int get() = 123
    }
    val g = object { fun H() {} }
}
fun I(block: () -> Unit) { block() }
fun J() {
    I {
        I {
            A()
        }
    }
}

fun used(x: Any?) {}
