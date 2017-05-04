fun box(x: Int, y: Int): Int {
    fun foo(
            z: Int) =
                x + z
    return foo(y)
}

// LINES: 2 4 2 5