fun main() {
    val a: List<Int>? = null
    print(<warning descr="SSR">a?.size</warning>)

    val b = listOf(1, 2, 3)
    print(b.size)
}