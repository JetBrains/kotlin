class A(val b: Boolean, val c: Int)

fun d(): A {
    return <warning descr="SSR">A(b = true, c = 0)</warning>
}