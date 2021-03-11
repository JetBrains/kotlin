class A {

    val x = <warning descr="SSR">B()</warning>
    val y = <warning descr="SSR">A.B()</warning>

    class B {}

}