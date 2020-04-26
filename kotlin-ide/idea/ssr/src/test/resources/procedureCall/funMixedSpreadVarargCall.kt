fun a(vararg b: Int): List<Int> {
    return b.toList()
}

fun b() {
    <warning descr="SSR">a(0, *intArrayOf(1, 2, 3), 4)</warning>
    return
}