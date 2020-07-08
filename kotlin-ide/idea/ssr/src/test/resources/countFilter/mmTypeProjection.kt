class X<A> {}
class Y<A, B> {}
class Z<A, B, C> {}

<warning descr="SSR">fun foo1(par: Int) { print(par) }</warning>
<warning descr="SSR">fun foo2(par: X<String>) { print(par) }</warning>
<warning descr="SSR">fun foo3(par: Y<String, Int>) { print(par) }</warning>

fun bar(par: Z<String, Int, Boolean>) { print(par) }