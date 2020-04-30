fun a(b: Boolean, c: Int = 0): Boolean {
    return b && c == 0
}

fun d(): Boolean {
    val e = <warning descr="SSR">a(true)</warning>
    val f = <warning descr="SSR">a(true, 1)</warning>
    return e && f
}