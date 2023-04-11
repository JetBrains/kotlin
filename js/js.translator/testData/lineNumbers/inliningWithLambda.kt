fun box() {
    println("1")
    foo {
        println("x")
    }
    println("2")
    foo {
        println("y")
    }
    println("3")
}

inline fun foo(f: () -> Unit) {
    println("before")
    f()
    println("after")
}

// LINES(JS):    1   11 2 2   14 14   4 4 16 16 6 6   14 14   8 8 16 16 10 10 13 13 13 13 13 17 14 14 15 15 16 16
// LINES(JS_IR): 1 1    2 2 * 14 14 * 4 4 16 16 6 6 * 14 14 * 8 8 16 16 10 10 13 13             14 14 15 15 16 16
