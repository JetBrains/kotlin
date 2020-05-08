fun foo(x: Int): Any = when(x) {
    0 -> 1
    else -> "1"
}

val a = <warning descr="SSR">foo(0) as Int</warning>
val b = <warning descr="SSR">foo(0) as kotlin.Int</warning>
val c = foo(1) as String