fun foo(vararg x : Any?) = print(x.hashCode())

<warning descr="SSR">fun<T, R> a(a: T, b: R, c: T) { foo(a, b, c) }</warning>

fun<R, T> b(a: T, b: R, c: T) { foo(a, b, c) }

fun<T, R> c(a: T, b: R, c: R) { foo(a, b, c) }

fun<T> d(a: T, b: T, c: T) { foo(a, b, c) }

fun e(a: Int, b: Int, c: Int) { foo(a, b, c) }