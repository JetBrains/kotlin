fun a(): List<Int> {
    val x = <warning descr="SSR">listOf(1)</warning>
    val y = listOf(1, 2)
    return if(x.size == 1) x else y
}