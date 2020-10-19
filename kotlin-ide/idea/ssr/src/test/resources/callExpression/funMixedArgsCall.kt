fun a(b: Boolean, c: Int): Boolean {
    return b && c == 0
}

fun d(): Boolean {
    return <warning descr="SSR">a(c = 0, b = true)</warning>
}