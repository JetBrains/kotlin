fun a(b: Boolean, c: Int): Boolean {
    return b && c == 0
}

fun b(): Boolean {
    val e = <warning descr="SSR">a(true, 0)</warning>
    return e
}