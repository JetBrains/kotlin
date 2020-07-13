val x = arrayOf(1, 2, 3)
val y = arrayListOf("a", "b")

fun foo() {
    print(<warning descr="SSR">x[0]</warning>)
    print(y[0])
}