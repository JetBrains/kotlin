class A(val b: Boolean, val c: Int, val d: Int)

fun d() {
    println(<warning descr="SSR">A(true, 0, 1)</warning>)
    println(<warning descr="SSR">A(b = true, c = 0, d = 1)</warning>)
    println(<warning descr="SSR">A(c = 0, b = true, d = 1)</warning>)
    println(<warning descr="SSR">A(true, d = 1, c = 0)</warning>)
}