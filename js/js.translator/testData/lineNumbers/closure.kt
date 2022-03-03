fun box(x: Int, y: Int): Int {
    fun foo(
            z: Int) =
                x + z
    return foo(y)
}

// LINES(JS):    2 2 2 4 4 1 6 2 2 5 5
// LINES(JS_IR):                   5 5 * 4 4
