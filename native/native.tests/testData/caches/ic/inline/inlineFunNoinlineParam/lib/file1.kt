package test

inline fun foo(x: Int, block: (Int) -> Int): Int = block(x)
