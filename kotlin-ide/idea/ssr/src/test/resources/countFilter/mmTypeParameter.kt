<warning descr="SSR">fun foo1() {}</warning>
<warning descr="SSR">fun <A> foo2(x: A) { print(x) }</warning>
<warning descr="SSR">fun <A, B> foo3(x: A, y: B) { print("$x$y") }</warning>

fun <A, B, C> bar(x: A, y: B, z: C) { print("$x$y$z") }