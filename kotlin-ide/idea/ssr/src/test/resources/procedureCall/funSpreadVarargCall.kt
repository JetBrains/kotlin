fun a(vararg b: Int): List<Int> {
    return b.toList()
}

fun b() {
    <warning descr="SSR">a(*intArrayOf(1, 2, 3))</warning>
    return
}