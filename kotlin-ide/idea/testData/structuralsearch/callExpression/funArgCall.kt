fun a(b: Boolean, c: Int): Boolean {
    return b && c == 0
}

fun d(e: Boolean, f: Int): Boolean {
    return e && f == 0
}

fun g(): Boolean {
    val h = <warning descr="SSR">a(true, 0)</warning>
    val i = d(true, 0)
    return h && i
}