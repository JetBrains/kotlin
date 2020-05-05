class A(val b: () -> Unit)

fun c() {
    println(<warning descr="SSR">A { println() }</warning>)
    println(
        A {
            println()
            println()
        }
    )
}