fun foo(vararg x : Any?) = print(x.hashCode())

<warning descr="SSR">fun<T> a(a: Int.(T) -> T) { foo(a) }</warning>

fun<R, T> b(a: T, b: R) { foo(a, b) }

fun<T> c(a: T, b: Int) { foo(a, b) }

fun d(a: Int.(Int) -> Int) { foo(a) }