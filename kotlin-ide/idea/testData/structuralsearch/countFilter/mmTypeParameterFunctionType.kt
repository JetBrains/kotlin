<warning descr="SSR">fun foo1(x : () -> Unit) { print(x) }</warning>
<warning descr="SSR">fun foo2(x : (Int) -> Unit) { print(x) }</warning>
<warning descr="SSR">fun foo3(x : (Int, String) -> Unit) { print(x) }</warning>

fun bar(x : (Int, String, Boolean) -> Unit) { print(x) }
