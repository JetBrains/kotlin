class A(val b: () -> Unit)

fun c(): A {
    return <warning descr="SSR">A { println() }</warning>
}