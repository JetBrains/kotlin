fun box(x: Int) {
    println(
            if (
                x > 100
            )
                42
            else
                foo()
    )
}

private inline fun foo(): Int {
    println("foo")
    return 23
}

// LINES: 3 3 4 3 6 13 3 14 2 13 14