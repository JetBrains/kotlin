class A(val b: Boolean, val c: Int = 0)

fun d(): A {
    val e = <warning descr="SSR">A(true)</warning>
    val f = <warning descr="SSR">A(true, 1)</warning>
    return if(e.b) e else f
}