class A(vararg val b: Int)

fun b(): A {
    return <warning descr="SSR">A(0, *intArrayOf(1, 2, 3), 4)</warning>
}