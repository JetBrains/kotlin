fun a(): List<Int> {
    val x = <warning descr="SSR">listOf(1)</warning>
    val y = <warning descr="SSR">listOf(1, 2)</warning>
    val z = arrayOf(1, 2)
    return if(x.size == 1 || z.size == 2) x else y
}