class A(vararg val b: Int)

fun c(): A {
    return <warning descr="SSR">A(*intArrayOf(1, 2, 3))</warning>
}