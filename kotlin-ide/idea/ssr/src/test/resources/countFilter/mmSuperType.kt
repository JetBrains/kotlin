interface IOne
interface ITwo
interface IThree

<warning descr="SSR">class C : IOne, ITwo</warning>
<warning descr="SSR">class B : IOne</warning>
<warning descr="SSR">class A</warning>

class D : IOne, ITwo, IThree
