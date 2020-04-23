fun a(b: Boolean, c: Int): Boolean {
    return b && c == 0
}

fun b(): Boolean {
    val e = <warning descr="SSR">a(c = 0, b = true)</warning>
    return e
}