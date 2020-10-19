val x = mapOf(1 to 1)
val y = mapOf("a" to 1)

fun foo() {
    print(x[1])
    print(<warning descr="SSR">y["a"]</warning>)
}