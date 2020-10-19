annotation class A(val x: Int)

<warning descr="SSR">@A(0) class C() { }</warning>

@A(1) class D() { }