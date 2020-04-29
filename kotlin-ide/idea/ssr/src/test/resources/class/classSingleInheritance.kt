interface A

abstract class B

<warning descr="SSR">class C : A, B()</warning>

<warning descr="SSR">class D : B(), A</warning>

<warning descr="SSR">class E : A</warning>

class F : B()

class G