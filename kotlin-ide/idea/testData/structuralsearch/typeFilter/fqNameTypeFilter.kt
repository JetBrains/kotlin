class Foo

<warning descr="SSR">val foo1 = A.B.Foo()</warning>
val bar1 = Foo()

class A {

    class Foo
    <warning descr="SSR">val foo2 = B.Foo()</warning>
    val bar2 = Foo()

    class B {

        class Foo
        <warning descr="SSR">val foo2 = Foo()</warning>
        val bar3 = A.Foo()

        class C {

                class Foo
                val bar4 = Foo()

            }
    }
}