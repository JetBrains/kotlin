<warning descr="SSR">val foo = { x: String, y: Int -> "$y" == x }</warning>

val bar1 = { x: Int, y: String -> "$x" == y }

val bar2 = { x: String, y: Int -> "$x $y" }