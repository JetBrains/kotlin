fun foo(arg: Int): Any = when(arg) {
    0 -> 1
    else -> "a"
}

val bar1 = <warning descr="SSR">foo(0) is Int</warning>
val bar2 = foo(1) !is String