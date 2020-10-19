<warning descr="SSR">val foo1 : () -> Unit = { }</warning>
<warning descr="SSR">val foo2 : (Int) -> Unit = { x -> print("$x") }</warning>
<warning descr="SSR">val foo3 : (Int, String) -> Unit = { x, y -> print("$x$y") }</warning>

val bar : (Int, String, Boolean) -> Unit = { x, y, z -> print("$x$y$z") }
