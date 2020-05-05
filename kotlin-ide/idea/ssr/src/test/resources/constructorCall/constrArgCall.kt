class A(val b: Boolean, val c: Int)

fun b() {
    println(<warning descr="SSR">A(true, 0)</warning>)
    println(A(false, 0))
    println(A(true, 1))
}