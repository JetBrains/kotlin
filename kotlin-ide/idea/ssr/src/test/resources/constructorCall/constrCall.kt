class A

fun b(): A {
    return <warning descr="SSR">A()</warning>
}