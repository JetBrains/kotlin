fun foo(x: Int): Any = when(x) {
    0 -> 1
    else -> "1"
}

val foo1 = <warning descr="SSR">foo(0) is Int</warning>