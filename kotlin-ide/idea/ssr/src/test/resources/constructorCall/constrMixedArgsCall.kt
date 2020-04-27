class A(val b: Boolean, val c: Int)

fun d(): A {
    return <warning descr="SSR">A(c = 0, b = true)</warning>
}