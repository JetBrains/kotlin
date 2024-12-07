fun box(x: Int, y: Int): Int {
    fun foo(
            z: Int) =
                x + z
    return foo(y)
}

// LINES(JS_IR): 1 1 5 5 2 1 3 4 4 4
