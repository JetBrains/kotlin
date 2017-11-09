fun box(x: Int, y: Int): Int {
    fun foo(
            z: Int) =
                x + z
    return foo(y)
}

// LINES: 2 2 2 4 4 6 2 2 5 5