<warning descr="SSR">fun <T, E, R> foo(f: T.(E) -> R) : (T, E) -> R = { t, e -> t.f(e) }</warning>

fun <T, E, R> bar(f: (T, E)) : (T, E) -> R = { TODO() }
