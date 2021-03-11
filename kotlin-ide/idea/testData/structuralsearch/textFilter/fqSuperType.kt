package test

open class Foo

<warning descr="SSR">class Bar : Foo()</warning>

class A {
    open class Foo
    class Bar2 : Foo()
}