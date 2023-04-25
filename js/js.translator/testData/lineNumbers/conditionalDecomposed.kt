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

// LINES(JS):    1   10 3 3 3 4 3 6   13 13 3 14 2 12 15 13 13 14 14
// LINES(JS_IR): 1 1  * 3     4   6 * 13 13   14 2 12 12 13 13 14 14
