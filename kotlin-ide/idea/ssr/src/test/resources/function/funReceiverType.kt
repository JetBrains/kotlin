<warning descr="SSR">fun <T, E, R> refactorReceiver(f: T.(E) -> R) : (T, E) -> R = { t, e -> t.f(e) }</warning>

fun <T, E, R> foo(f: (T, E) -> R) : (T, E) -> R = { t, e -> f(t, e) }