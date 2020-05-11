<warning descr="SSR">fun foo1(p: Int) { print(p) }</warning>
<warning descr="SSR">fun foo2(p: String) { print(p) }</warning>
<warning descr="SSR">fun foo3(p: (Int) -> Int) { print(p(0)) }</warning>
fun bar(p1: Int, p2: Int) { print(p1 + p2) }