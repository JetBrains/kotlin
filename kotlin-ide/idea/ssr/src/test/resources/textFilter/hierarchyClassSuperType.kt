open class Foo

interface FooI

<warning descr="SSR">open class Bar1 : Foo()</warning>

<warning descr="SSR">class Bar2 : Bar1(), FooI</warning>