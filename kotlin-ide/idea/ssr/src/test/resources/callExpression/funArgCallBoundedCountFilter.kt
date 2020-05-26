fun a(): List<Int> {
    val x = listOf(1)
    val y = listOf(1, 2)
    val z = <warning descr="SSR">listOf(1, 2, 3, 4)</warning>
    return if(x.size == 1 || z.size == 2) x else y
}