fun foo(x: Int): Any = when(x) {
    0 -> 1
    else -> "1"
}

val foo1 = <warning descr="SSR">foo(0) as Int</warning>
val foo2 = <warning descr="SSR">foo(1) as String</warning>
val bar1 = foo(1) as? String
val bar2 = foo(1) is String