class A(val b: Boolean, val c: Int, val d: Int)

fun b() {
    println(<warning descr="SSR">A(true, 0, 1)</warning>)
    println(A(false, 0, 1))
    println(A(true, 1, 1))
    println(<warning descr="SSR">A(b = true, c = 0, d = 1)</warning>)
    println(<warning descr="SSR">A(c = 0, d = 1, b = true)</warning>)
    println(<warning descr="SSR">A(true, d = 1, c = 0)</warning>)
}