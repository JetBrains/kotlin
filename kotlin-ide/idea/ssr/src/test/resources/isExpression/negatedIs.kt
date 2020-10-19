fun foo(arg: Int): Any = when(arg) {
    0 -> 1
    else -> "a"
}

val bar1 = foo(0) is Int
val bar2 = <warning descr="SSR">foo(1) !is String</warning>