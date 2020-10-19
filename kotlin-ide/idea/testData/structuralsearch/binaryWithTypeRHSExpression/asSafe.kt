fun foo(x: Int): Any = when(x) {
    0 -> 1
    else -> "1"
}

val foo1 = foo(0) as Int
val foo2 = foo(1) as String
val bar1 = <warning descr="SSR">foo(1) as? String</warning>
val bar2 = foo(1) is String