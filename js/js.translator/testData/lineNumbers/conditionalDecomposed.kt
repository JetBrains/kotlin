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

// LINES: 10 3 3 3 4 3 6 13 13 3 14 2 15 13 13 14 14