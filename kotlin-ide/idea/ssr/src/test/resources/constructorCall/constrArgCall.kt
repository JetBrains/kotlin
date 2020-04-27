class A(val b: Boolean, val c: Int)

fun b(): A {
    return <warning descr="SSR">A(true, 0)</warning>
}