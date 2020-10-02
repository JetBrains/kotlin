class X {
    open class Foo
    <warning descr="SSR">open class Foo2: Foo()</warning>
    <warning descr="SSR">class Foo3 : Foo2()</warning>
}