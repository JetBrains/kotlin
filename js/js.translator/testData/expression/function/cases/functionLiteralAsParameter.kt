package foo

fun apply(f: (Int) -> Int, t: Int): Int {
    return f(t)
}


fun box(): Boolean {
    return apply({ a: Int -> a + 5 }, 3) == 8
}