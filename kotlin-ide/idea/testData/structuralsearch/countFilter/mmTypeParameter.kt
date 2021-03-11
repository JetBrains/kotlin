<warning descr="SSR">fun foo1() {}</warning>
<warning descr="SSR">fun <A> foo2(x: A) { x.hashCode() }</warning>
<warning descr="SSR">fun <A, B> foo3(x: A, y: B) { x.hashCode() + y.hashCode() }</warning>

fun <A, B, C> bar(x: A, y: B, z: C) { x.hashCode() + y.hashCode() + z.hashCode() }