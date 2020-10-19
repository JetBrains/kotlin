interface IOne
interface ITwo
interface IThree

<warning descr="SSR">open class A</warning>
<warning descr="SSR">class B : IOne</warning>
<warning descr="SSR">class B2 : IOne, A()</warning>
<warning descr="SSR">class C : IOne, ITwo</warning>
<warning descr="SSR">class C2 : IOne, ITwo, A()</warning>

class D : IOne, ITwo, IThree
class D2 : IOne, ITwo, IThree, A()
