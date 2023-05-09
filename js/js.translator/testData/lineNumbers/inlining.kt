fun box() {
    println("1")
    bar()
    println("2")
    bar()
    println("3")
}

inline fun bar() {
    println("bar1")
    println("bar2")
}

// LINES(JS):    1 7 2 2   10 10 11 11 4 4   10 10 11 11 6 6 9 9 9 9 9 12 10 10 11 11
// LINES(JS_IR): 1 1 2 2 * 10 10 11 11 4 4 * 10 10 11 11 6 6 9 9          10 10 11 11
