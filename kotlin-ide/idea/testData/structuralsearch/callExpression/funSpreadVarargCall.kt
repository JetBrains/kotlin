fun a(vararg b: Int): List<Int> {
    return b.toList()
}

fun c(): List<Int> {
    return <warning descr="SSR">a(*intArrayOf(1, 2, 3))</warning>
}