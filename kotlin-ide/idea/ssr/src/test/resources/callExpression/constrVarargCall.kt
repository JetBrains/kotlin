class A(vararg val b: Int)

fun c(): A {
    return <warning descr="SSR">A(1, 2, 3)</warning>
}