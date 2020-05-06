class A(val b: Boolean, val c: Int)

fun d() {
    println(<warning descr="SSR">A(true, 0)</warning>)
    println(<warning descr="SSR">A(b = true, c = 0)</warning>)
}