class A(vararg val b: Int)

fun c(): A {
    println(A(0, 1, 2, 3, 4, 5))
    println(A(1, 2, 3, 4, 5))
    println(A(0, 1, 3, 4))
    println(A(0, 2, 3, 4))
    println(A(0, 2, 3, 4))
    println(A(0))
    return <warning descr="SSR">A(0, 1, 2, 3, 4)</warning>
}