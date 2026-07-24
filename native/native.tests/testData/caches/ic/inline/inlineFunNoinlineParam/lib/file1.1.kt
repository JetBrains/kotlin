package test

inline fun foo(x: Int, noinline block: (Int) -> Int): Int = block(x)
