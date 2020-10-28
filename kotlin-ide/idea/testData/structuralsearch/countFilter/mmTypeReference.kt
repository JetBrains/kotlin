<warning descr="SSR">val foo1 : () -> String = { "" }</warning>
<warning descr="SSR">val foo2 : (Int) -> String = { x -> "$x" }</warning>
<warning descr="SSR">val foo3 : (Int, String) -> String = { x, y -> "$x$y" }</warning>

val bar : (Int, String, Boolean) -> String = { x, y, z -> "$x$y$z" }
